package com.esprit.employee.staff;

import com.esprit.employee.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A concrete link between an employee and a confirmed reservation for a given time slot.
 * <p>
 * This is what makes "is this employee free?" a real, time-aware question: an employee is busy
 * for a slot only when they already hold {@code MAX_RESERVATIONS_PER_EMPLOYEE} assignments
 * overlapping that slot. The row is created when a {@code reservation.confirmed} event arrives,
 * so the database reflects real bookings rather than a global ACTIVE flag.
 */
@Entity
@Table(name = "staff_assignment",
        uniqueConstraints = @UniqueConstraint(name = "uq_assignment_reservation_code", columnNames = "reservation_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 40)
    private String reservationCode;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "guests_count", nullable = false)
    private int guestsCount;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime assignedAt;
}
