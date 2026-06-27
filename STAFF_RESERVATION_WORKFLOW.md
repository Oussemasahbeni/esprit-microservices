# Staff ↔ Reservation Integration

How the **Reservation** service (MS3) and the **Employee Management** service stay in sync so a
booking is never confirmed without knowing whether there are waiters free to serve it.

Two communication channels:

| Channel | Direction | Purpose |
| --- | --- | --- |
| **Feign (sync)** | Reservation → Employee | Ask *right now*: "are any staff free for this slot?" before confirming. |
| **RabbitMQ (async)** | Reservation → Employee | After confirming, actually **assign** a real employee to the booking. |
| **RabbitMQ (async)** | Employee → Reservation | When an employee is enabled/disabled, broadcast the new staff level. |

---

## Core rules

- An employee has an account `status`: `ACTIVE` / `INACTIVE` / `SUSPENDED`. Only `ACTIVE` employees can be assigned.
- A reservation occupies a **2-hour slot** (`SLOT_DURATION_HOURS = 2`).
- One `ACTIVE` employee can serve up to **10 overlapping reservations** at the same time (`MAX_RESERVATIONS_PER_EMPLOYEE = 10`).
- An employee is **free** for a slot only while holding **fewer than 10** assignments overlapping `[start, start+2h)`.
- Assignment is **idempotent** per `reservationCode` (the same confirmed event handled twice does not double-book).

---

## Database changes

### `staff_assignment` (employee-management) — NEW
The real, persisted link that makes "is this employee free?" a time-aware question.

| Column | Meaning |
| --- | --- |
| `id` | PK |
| `employee_id` | FK → `employee` |
| `reservation_code` | UNIQUE — idempotency key from the reservation event |
| `reservation_date`, `start_time`, `end_time` | the 2-hour slot served |
| `guests_count` | guests on that booking |
| `assigned_at` | audit timestamp |

**Overlap test** used to count an employee's load:
`existing.start_time < slot.end AND existing.end_time > slot.start` on the same `reservation_date`.

### Tables written per workflow

| Trigger | Table change |
| --- | --- |
| `reservation.confirmed` consumed | INSERT one `staff_assignment` row (or none + STAFF ALERT log if all at capacity) |
| Employee enable/disable | UPDATE `employee.status` (ACTIVE/INACTIVE) |

---

## Workflow per API / event

### 1. `POST /api/reservations` (Reservation) — create a booking
```
Client → ReservationController.createReservation
      → ReservationService.createReservation
          1. validate date is in the future
          2. SYNC Feign: EmployeeManagementClient.checkStaffAvailability(dateTime)
                 → GET /api/staff/availability  (Employee service)
                 → StaffAvailabilityService.checkAvailability
                 → StaffAssignmentService.countAvailableStaff(date, time)
                       counts ACTIVE employees whose overlapping load < 10
                 → { availableStaff, sufficient }
             staffWarning = !sufficient   (booking still proceeds, manager warned)
          3. AvailabilityService.findAvailableTables → pick smallest fitting table
          4a. table found  → save Reservation (CONFIRMED)
                           → publish reservation.confirmed  (RabbitMQ)
          4b. no table     → save WaitlistEntry (WAITING)
                           → publish reservation.waitlisted
```
- **DB:** INSERT `reservation` (+ status history) **or** INSERT `waitlist_entry`.
- If the Employee service is down, the Feign call fails safe → assumes sufficient staff (`availableStaff = -1`), booking continues.

### 2. `reservation.confirmed` event (Employee consumes) — assign a real waiter
```
RabbitMQ esprit.events / reservation.confirmed
      → reservation.confirmed.for.employee queue
      → ReservationEventConsumer.onReservationConfirmed
          → StaffAssignmentService.assignStaff(code, date, start, guests)
              - idempotent: skip if code already assigned
              - pick the LEAST-LOADED ACTIVE employee with load < 10
              - INSERT staff_assignment
              - none free → log "STAFF ALERT" for the manager
```
- **DB:** INSERT `staff_assignment` (or no-op + alert log).
- **Effect:** that employee's remaining capacity for the slot drops → future `countAvailableStaff` reflects it.

### 3. `GET /api/staff/availability?dateTime=...` (Employee) — sync capacity check
```
StaffAvailabilityController.checkAvailability
      → StaffAvailabilityService.checkAvailability
      → StaffAssignmentService.countAvailableStaff(date, time)
      → { dateTime, availableStaff, sufficient }   (sufficient = availableStaff >= 1)
```
- **DB:** read-only (counts assignments per active employee).
- Called by Reservation via Feign in workflow 1.

### 4. Employee enable / disable (Employee) — staff level changes
```
EmployeeService.enable(id) / disable(id)
      → update Keycloak + employee.status
      → ScheduleEventPublisher.publishScheduleUpdated(today, activeCount, userId)
      → publish employee.schedule.updated  (RabbitMQ)
            → schedule.updates.for.reservation queue (Reservation service consumes)
```
- **DB:** UPDATE `employee.status`.
- Lets the Reservation service warn managers when the night becomes understaffed.

---

## End-to-end picture

```
Client books Friday 20:00
   │  POST /api/reservations
   ▼
ReservationService ──Feign──▶ GET /api/staff/availability
                                 countAvailableStaff(Fri, 20:00) → 0 free? sufficient=false
   │  staffWarning=true → CONFIRMED anyway (manager warned)
   ▼
publish reservation.confirmed ──RabbitMQ──▶ ReservationEventConsumer
                                              assignStaff() → least-loaded ACTIVE employee < 10 → INSERT staff_assignment
                                              (or STAFF ALERT if all at capacity)

Manager disables an employee
   │
   ▼
publish employee.schedule.updated ──RabbitMQ──▶ Reservation service → warn manager
```

---

## Tuning knobs
`employee-management/.../staff/StaffAssignmentService.java`:
- `MAX_RESERVATIONS_PER_EMPLOYEE = 10` — concurrent reservations one waiter can serve.
- `SLOT_DURATION_HOURS = 2` — reservation slot length used for overlap.

## Tests
- `StaffAssignmentServiceTest` — capacity counting, least-loaded pick, full-capacity, idempotency.
- `StaffAvailabilityServiceTest` — sync response mapping.
- `ReservationEventConsumerTest` — assignment delegation + no-staff path.
