# Esprit Microservices — Changes in this branch

This document explains every uncommitted change in this working tree: what was broken, what was
built, why, and how the pieces fit together. Nothing here has been committed yet — this is a
working log to review before you commit.

## Table of contents

1. [Architecture overview](#1-architecture-overview)
2. [Configuration: everything now lives in Consul](#2-configuration-everything-now-lives-in-consul)
3. [RabbitMQ: menu → reservation event flow](#3-rabbitmq-menu--reservation-event-flow)
4. [New entities & business logic in `reservation`](#4-new-entities--business-logic-in-reservation)
5. [Docker](#5-docker)
6. [Frontend (esprit-microservices-front)](#6-frontend-esprit-microservices-front)
7. [Known issue — frontend production build](#7-known-issue--frontend-production-build)
8. [Full file-by-file change list](#8-full-file-by-file-change-list)

---

## 1. Architecture overview

```mermaid
flowchart TB
    FE[Angular frontend] -->|HTTP, all calls| GW[gateway :8080]

    GW --> EMP[employee-management :8084]
    GW --> MENU[menu-management :8085]
    GW --> DELIV[delivery-management :8086]
    GW --> RES[reservation :8083]

    subgraph Infra
      CONSUL[(Consul KV\nconfig server)]
      RABBIT[(RabbitMQ\nesprit.events exchange)]
      PG[(Postgres)]
      MYSQL[(MySQL)]
      KC[(Keycloak)]
    end

    EMP -. reads config .-> CONSUL
    MENU -. reads config .-> CONSUL
    DELIV -. reads config .-> CONSUL
    RES -. reads config .-> CONSUL
    GW -. reads config .-> CONSUL

    MENU -- publishes dish.*/promo.* --> RABBIT
    RABBIT -- delivers dish.*/promo.* --> RES
    RES -- publishes reservation.*/employee.* --> RABBIT
    RABBIT -- delivers --> EMP

    EMP --> PG
    DELIV --> PG
    RES --> RES_PG[(reservation-postgres)]
    MENU --> MYSQL
    GW --> KC
```

The frontend **never** talks to a microservice directly — every request goes through the
`gateway`, which routes by path (see `gateway/.../GatewayRoutesConfig.java`) and validates the JWT
against Keycloak.

---

## 2. Configuration: everything now lives in Consul

**This is the part that was wrong and is now fixed — read this section carefully.**

### What was wrong

Early in this change, `server.port`, RabbitMQ host/port, and a Keycloak issuer-uri block were added
directly into `reservation/application.yaml` and `gateway/application.yaml`. That was a mistake:
**this project's actual config server is Consul KV** (`consul-kv.json` at the repo root is a
snapshot of it, imported via `consul kv import @consul-kv.json` after Consul starts). Every service
already does:

```yaml
spring:
  config:
    import: consul:${CONSUL_HOST:localhost}:${CONSUL_PORT:8500}
  cloud:
    consul:
      config:
        enabled: true
        format: YAML
        prefixes: config
        default-context: application
```

…which pulls `config/<spring.application.name>/data` from Consul as a YAML blob and merges it in.
Adding `server.port: 8083` directly to `reservation/application.yaml` **duplicated** a value Consul
*already* provides for `config/reservation/data` — and because Spring Boot gives properties already
present in the importing file priority over imported ones, the local duplicate would have silently
shadowed Consul's copy. That's exactly backwards from how this project is meant to work.

### What's fixed now

Every one of the 5 services' `application.yaml` has been stripped down to **only the bootstrap**
needed to reach Consul:

```yaml
spring:
  application:
    name: <service-name>
  config:
    import: consul:${CONSUL_HOST:localhost}:${CONSUL_PORT:8500}
  cloud:
    consul:
      config:
        enabled: true
        format: YAML
        prefixes: config
        default-context: application
```

(`gateway` additionally keeps its `resilience4j` circuit-breaker tuning and `logging.level` locally
— those are code-level tuning knobs, not deployment config, so they stay out of Consul on purpose.)

Everything else — `server.port`, datasource URL/credentials, JPA/Flyway settings, RabbitMQ
host/port/credentials, Keycloak issuer-uri/client-secret, springdoc settings — now lives **only** in
`consul-kv.json`, one entry per service under `config/<service>/data` (base64-encoded YAML, matching
the format Consul KV expects):

```mermaid
flowchart LR
    subgraph "consul-kv.json (imported once via consul kv import)"
      A["config/reservation/data"]
      B["config/employee-management/data"]
      C["config/menu-management/data"]
      D["config/delivery-management/data"]
      E["config/gateway/data"]
    end
    A --> CONSUL[(Consul KV store)]
    B --> CONSUL
    C --> CONSUL
    D --> CONSUL
    E --> CONSUL
    CONSUL -->|spring.config.import: consul:...| RES[reservation]
    CONSUL --> EMP[employee-management]
    CONSUL --> MENU[menu-management]
    CONSUL --> DELIV[delivery-management]
    CONSUL --> GW[gateway]
```

### Per-service Consul KV entries

| Key | Status | What it carries |
|---|---|---|
| `config/reservation/data` | **updated** | port 8083, Postgres datasource, Flyway, problem-details, **+ new RabbitMQ block**, Keycloak server-url, springdoc |
| `config/gateway/data` | unchanged | port 8080, OAuth2 resource-server issuer-uri, the gateway route table, springdoc/swagger oauth |
| `config/employee-management/data` | **new** | port 8084, Postgres datasource, OAuth2 issuer-uri, **+ new RabbitMQ block**, full Keycloak admin-client block, springdoc |
| `config/menu-management/data` | **new** | port 8085, MySQL datasource, RabbitMQ block (mirrors what was already hardcoded locally) |
| `config/delivery-management/data` | **new** | port 8086, Postgres datasource, Flyway |
| `config/demo1/data`, `config/demo2/data`, `config/application/data` | untouched | pre-existing demo modules / shared defaults, not part of this change |

The encoding of `consul-kv.json` itself (UTF-16LE with BOM, the format Windows PowerShell produces)
was preserved exactly — it was edited with PowerShell's `ConvertFrom-Json`/`ConvertTo-Json` round
trip, not rewritten from scratch, specifically so the file's byte format doesn't change.

### Why RabbitMQ config was missing in the first place

`reservation` and `employee-management` both have `@RabbitListener`/`RabbitTemplate` beans, but
**neither had a `spring.rabbitmq.*` property anywhere** (not locally, not in Consul). Spring Boot's
RabbitMQ autoconfiguration silently falls back to `localhost:5672` with no error — which works by
accident on a local dev machine where RabbitMQ happens to be on localhost, and breaks silently the
moment either service runs in a container next to a separate `rabbitmq` container. This is now
fixed by adding the missing `spring.rabbitmq.host/port/username/password` block (driven by
`RABBITMQ_HOST`/`RABBITMQ_PORT`/etc. env vars, same pattern `menu-management` already used) into
both services' Consul KV entries.

---

## 3. RabbitMQ: menu → reservation event flow

Before this change, `menu-management` published `dish.unavailable` and `promo.created` to the
`esprit.events` topic exchange — **with zero consumers anywhere in the system.** It was dead code.

```mermaid
sequenceDiagram
    participant Manager as Manager (menu UI)
    participant Menu as menu-management
    participant Rabbit as esprit.events exchange
    participant Res as reservation

    Manager->>Menu: create/update/delete dish
    Menu->>Rabbit: publish dish.created / dish.updated / dish.deleted
    Rabbit->>Res: deliver to dish.sync.for.reservation queue
    Res->>Res: upsert MenuDishCache row

    Manager->>Menu: create promotion
    Menu->>Rabbit: publish promo.created
    Rabbit->>Res: deliver to promo.sync.for.reservation queue
    Res->>Res: upsert MenuPromotionCache row

    Note over Res: If a dish goes unavailable and is pre-ordered<br/>on an upcoming CONFIRMED/SEATED reservation,<br/>that ReservationPreOrderItem.stillAvailable -> false
```

### What was added

**`menu-management`** (publisher side):
- `DishSyncEvent` — full catalog snapshot (id, name, price, available, category) published on
  `dish.created` / `dish.updated` / `dish.deleted`.
- `MenuEventPublisher.publishDishCreated/Updated/Deleted` — wired into `MenuService` so every
  create/update/delete actually emits an event (previously only the narrow "went unavailable" case
  published anything).

**`reservation`** (consumer side, the part that genuinely didn't exist before):
- `RabbitMqConfig` — two new durable queues, each with a dead-letter queue for poison messages:
  - `dish.sync.for.reservation`, bound to the **explicit** routing keys `dish.created`,
    `dish.updated`, `dish.deleted` (deliberately **not** a `dish.*` wildcard — `dish.unavailable`
    carries a different, narrower payload shape than `DishSyncEvent`, and wildcard-binding it would
    have broken JSON deserialization on that queue).
  - `promo.sync.for.reservation`, bound to `promo.*`.
- `MenuEventConsumer` — the actual `@RabbitListener` methods. Upserts `MenuDishCache` /
  `MenuPromotionCache`, and on a dish transitioning from available→unavailable, flags any upcoming
  pre-ordered reservation item as `stillAvailable = false` (see §4).
- Mirrored DTOs (`DishSyncEvent`, `DishUnavailableEvent`, `PromotionSyncEvent`) — each microservice
  keeps its own copy of the wire-format record rather than sharing a library, matching the existing
  convention already used for `ReservationConfirmedEvent` in `employee-management`.

> **Note on a removed feature:** an earlier iteration of this change also added a persisted
> `ManagerAlert` table + REST endpoints (`GET/PATCH /api/manager/alerts`) to surface "dish you
> pre-ordered just went unavailable" as actionable alerts. That was explicitly **removed** per
> feedback — it added a notification layer nobody asked for. What's left is the useful part: the
> pre-order item itself gets flagged `stillAvailable = false`, visible directly on the reservation
> (both in "My reservations" and the manager dashboard), with no separate alert system on top.

---

## 4. New entities & business logic in `reservation`

```mermaid
erDiagram
    Reservation ||--o{ ReservationPreOrderItem : "has"
    Reservation ||--o{ ReservationStatusHistory : "has"
    Reservation }o--|| RestaurantTable : "assigned to"
    MenuDishCache ||..o{ ReservationPreOrderItem : "snapshot source (by id, not FK)"
    MenuPromotionCache {
      bigint id PK
      string name
      decimal discountPercent
      bigint dishId
      bigint categoryId
      datetime startsAt
      datetime endsAt
    }
    MenuDishCache {
      bigint id PK "same id as menu-management's dish"
      string name
      decimal price
      boolean available
      bigint categoryId
      string categoryName
    }
    ReservationPreOrderItem {
      bigint id PK
      bigint reservation_id FK
      bigint menuDishId
      string dishName "snapshotted at order time"
      decimal unitPrice "snapshotted at order time"
      int quantity
      boolean stillAvailable
    }
```

| Class | Purpose |
|---|---|
| `MenuDishCache` | Local read-model of menu-management's dish catalog. Populated entirely from RabbitMQ — `reservation` never calls `menu-management` synchronously to render the booking form's menu. |
| `MenuPromotionCache` | Same idea, for promotions. |
| `ReservationPreOrderItem` | A dish a customer pre-ordered when booking. Snapshots `dishName`/`unitPrice` at order time so the record stays meaningful even if the dish is later renamed, repriced, or deleted upstream. `stillAvailable` flips to `false` if the dish goes unavailable after the booking was made. |
| `MenuSnapshotService` | Builds the "available dishes + active promotions" view (`GET /api/reservations/menu`) the booking UI calls — purely from the local cache. |

### Business logic added to `ReservationService.createReservation(...)`

1. New parameter: `List<PreOrderItemRequest> preOrderItemRequests`.
2. `resolvePreOrderItems(...)` validates every requested `dishId` exists in `MenuDishCache` and is
   currently `available` — throws `IllegalArgumentException` (HTTP 400 via the existing
   `GlobalExceptionHandler`) otherwise. No partial/silent failures.
3. On the CONFIRMED path (a table was found), the resolved pre-order items are attached to the
   `Reservation` and persisted via cascade. On the waitlist path, pre-order is currently dropped
   (a customer who gets waitlisted doesn't have a confirmed slot yet to attach dishes to).

### New REST surface on `reservation`

| Method | Path | What |
|---|---|---|
| `GET` | `/api/reservations/menu` | Cached available dishes + active promotions, for the booking form |
| `POST` | `/api/reservations` | *(existing endpoint, extended)* now accepts `preOrderItems: [{dishId, quantity}]` |

### Flyway migration

`reservation/src/main/resources/db/migration/V3__menu_sync_and_preorders.sql` creates
`menu_dish_cache`, `menu_promotion_cache`, `reservation_pre_order_item` (with an FK to
`reservation` and an index on `menu_dish_id`).

---

## 5. Docker

**`docker-compose.yml` was restored to its original committed state and left untouched.** It was
deleted in an earlier pass of this change without asking first — that was wrong, and it's back
exactly as it was.

`compose.yaml` is the file Docker actually uses by default when both exist in the same directory
(`docker compose up` prefers `compose.yaml` over `docker-compose.yml`). It already had the more
complete, dedicated-database-per-service setup (separate `reservation-postgres`, `menu-mysql`,
Keycloak realm-import from `./keycloak/kc-realm`). The following gaps in `compose.yaml` were closed:

- **RabbitMQ was entirely missing** — `compose.yaml` had no `rabbitmq` service at all. Added.
- **No service definitions for `gateway`, `employee-management`, `menu-management`,
  `delivery-management`** — the file only ran `reservation` plus infra. All four added, wired to
  the matching Consul KV entries from §2.
- **`KEYCLOAK_URL` env var** on `reservation` didn't match any property the app actually reads
  (`KEYCLOAK_ISSUER_URI` is what's wired) — fixed wherever it recurs.

### Dockerfiles (new)

A `Dockerfile` was added per service (`gateway/Dockerfile`, `reservation/Dockerfile`,
`employee-management/Dockerfile`, `menu-management/Dockerfile`, `delivery-management/Dockerfile`).
None were ever built into an image (per instruction) — only created, multi-stage:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build   # compiles just that module via -pl <module> -am
FROM eclipse-temurin:21-jre-alpine AS runtime # copies the jar, runs as non-root
```

They all build from the **repository root** (`context: .`, `dockerfile: <service>/Dockerfile`) —
required because this is a multi-module Maven reactor: the parent `pom.xml` and every sibling
module's `pom.xml` have to be visible to Maven even though only one module is actually compiled.

---

## 6. Frontend (`esprit-microservices-front`)

A full `features/reservations/` module was added, following the exact conventions already used by
`features/menu/` and `features/employees/` (signals, `httpResource`, `inject()`, `OnPush`, reactive
forms, Transloco i18n scopes) — **menu-management's frontend was explicitly left untouched**, per
instruction.

```mermaid
flowchart TB
    subgraph "features/reservations/"
      BOOK[book-reservation\nbooking form + pre-order picker]
      MY[my-reservations\nlist + cancel]
      MGR[manager-reservations\nseat/complete/no-show, waitlist, daily stats]
      TBL[tables-rooms\nroom + table CRUD, table status]
      LOOK[lookup-reservation\nguest lookup by confirmation code]
    end
    SVC[ReservationService] --> GW[gateway :8080]
    BOOK --> SVC
    MY --> SVC
    MGR --> SVC
    TBL --> SVC
    LOOK --> SVC
```

| Page | Route | Backend endpoints it consumes |
|---|---|---|
| Book a table | `/reservations/book` | `GET /api/reservations/menu`, `POST /api/reservations` |
| My reservations | `/reservations/my` | `GET /api/reservations/my`, `DELETE /api/reservations/{id}/cancel` |
| Manager dashboard | `/reservations/manager` | `GET/PATCH /api/manager/reservations/**`, `GET/POST/DELETE /api/manager/waitlist/**`, `GET /api/manager/stats` |
| Tables & rooms | `/reservations/tables` | `GET/POST /api/rooms`, `GET/POST /api/tables`, `PATCH /api/tables/{id}/status` |
| Look up by code | `/reservations/lookup` | `GET /api/reservations/code/{code}` |

This now covers essentially the full REST surface of the `reservation` microservice (rooms/tables
management and the daily stats panel were the two pieces missing in the first pass). Registered in
`app.routes.ts` and the sidebar nav; `en`/`fr`/`ar` i18n scopes added under
`public/i18n/reservations/` (fr/ar currently mirror the English copy — they still need real
translation, flagged here rather than silently shipped as if they were localized).

`shared/models/reservation.ts` and `features/reservations/service/reservation.service.ts` hold the
TypeScript types and the single `HttpClient`-based service all five pages share — every call goes
to `environment.apiUrl` (the gateway), never to a microservice directly.

---

## 7. Known issue — frontend production build

`ng build --configuration production` is currently failing with:

```
[FAILED: Unexpected token ';']
```

This was investigated at length:

- `tsc -p tsconfig.json --noEmit` finds **zero** errors across the whole project.
- The error's stack trace (`compileSourceTextModule` → `ModuleLoader.getModuleJobForRequire` →
  `importSyncForRequire`) is Node's own ESM loader choking while `require()`-ing something as a
  module — not an Angular/TypeScript compile error pointing at a specific file.
- It reproduced identically regardless of which combination of the new feature files
  (`tables-rooms`, the manager-dashboard stats panel, the i18n additions, the nav/route wiring) was
  temporarily quarantined and rebuilt — including configurations that had previously built clean
  earlier in this same session with the *same* `node_modules`.
- `node_modules`, `dist`, and `.angular` were removed as part of cleanup before this could be fully
  root-caused.

**Next step:** run `pnpm install` fresh and re-run `ng build --configuration production`. Given
`tsc` is clean and two earlier full builds (with most of this feature already in place) succeeded
in this same session, this looks like a tooling/Node-ESM-loader interaction rather than a defect in
the code that was added — but that should be confirmed with a clean install rather than taken on
faith.

---

## 8. Full file-by-file change list

### Backend — `menu-management` (publisher side, made the existing RabbitMQ wiring real)
- `messaging/RabbitMqConfig.java` — *modified*: added `dish.created`/`dish.updated`/`dish.deleted` routing keys.
- `messaging/MenuEventPublisher.java` — *modified*: added `publishDishCreated/Updated/Deleted`.
- `messaging/DishSyncEvent.java` — *new*: full dish snapshot event record.
- `service/MenuService.java` — *modified*: calls the new publish methods from create/update/delete.
- `test/.../MenuServiceTest.java` — *modified*: updated to assert `publishDishCreated` is actually called (was asserting `verifyNoInteractions`, which was true only because the event didn't exist yet).
- `application.yaml` — *modified*: stripped to Consul bootstrap only (see §2).

### Backend — `reservation` (consumer side + pre-order feature)
- `config/RabbitMqConfig.java` — *modified*: new `dish.sync.for.reservation` / `promo.sync.for.reservation` queues + DLQs + bindings.
- `messaging/MenuEventConsumer.java` — *new*: the `@RabbitListener`s.
- `messaging/DishSyncEvent.java`, `DishUnavailableEvent.java`, `PromotionSyncEvent.java` — *new*: mirrored wire-format DTOs.
- `entity/MenuDishCache.java`, `MenuPromotionCache.java`, `ReservationPreOrderItem.java` — *new* (see §4).
- `entity/Reservation.java` — *modified*: added `preOrderItems` collection.
- `repository/MenuDishCacheRepository.java`, `MenuPromotionCacheRepository.java`, `ReservationPreOrderItemRepository.java` — *new*.
- `service/MenuSnapshotService.java` — *new*: builds the booking form's menu view.
- `service/ReservationService.java` — *modified*: `createReservation(...)` now resolves/validates/attaches pre-order items.
- `dto/PreOrderItemRequest.java`, `PreOrderItemResponse.java`, `MenuSnapshotResponse.java` — *new*.
- `dto/CreateReservationRequest.java`, `ReservationResponse.java` — *modified*: added pre-order fields.
- `mapper/ReservationMapper.java` — *modified*: maps `ReservationPreOrderItem` → `PreOrderItemResponse`.
- `controller/ReservationController.java` — *modified*: passes `preOrderItems` through; new `GET /menu`.
- `db/migration/V3__menu_sync_and_preorders.sql` — *new*.
- `application.yaml` — *modified*: stripped to Consul bootstrap only.
- `test/.../ReservationServiceTest.java`, `ReservationServiceStaffCheckTest.java` — *modified*: updated for the new `createReservation` signature; also fixed a **pre-existing** signature mismatch with `EmployeeManagementClient`/`StaffAvailabilityResponse` that was already broken before this change (unrelated to this feature, fixed because it blocked the whole test module from compiling).

### Backend — `employee-management`, `gateway`, `delivery-management`
- `employee-management/application.yaml` — *modified*: stripped to Consul bootstrap only.
- `gateway/application.yaml` — *modified*: removed the duplicated `server.port`/OAuth2-issuer-uri block, stripped to bootstrap + resilience4j + logging.
- `delivery-management/application.yaml` — *modified*: stripped to Consul bootstrap only.

### Infra
- `consul-kv.json` — *modified*: added RabbitMQ block to `config/reservation/data`; added three new entries (`config/employee-management/data`, `config/menu-management/data`, `config/delivery-management/data`). Encoding (UTF-16LE+BOM) preserved.
- `postgres-init/create-keycloak-db.sql` — *modified*: dropped the now-redundant `CREATE DATABASE reservation_db` line (reservation has its own dedicated Postgres container in `compose.yaml`, not this shared one).
- `compose.yaml` — *modified*: added `rabbitmq`, `gateway`, `employee-management`, `menu-management`, `delivery-management` services; fixed the `KEYCLOAK_URL` → `KEYCLOAK_ISSUER_URI` mismatch.
- `docker-compose.yml` — **untouched**, restored after being wrongly deleted earlier in this change.
- `gateway/Dockerfile`, `reservation/Dockerfile`, `employee-management/Dockerfile`, `menu-management/Dockerfile`, `delivery-management/Dockerfile` — *new*, not built into images.

### Frontend — `esprit-microservices-front`
- `src/app/shared/models/reservation.ts` — *new*.
- `src/app/features/reservations/service/reservation.service.ts` — *new*.
- `src/app/features/reservations/pages/{book-reservation,my-reservations,manager-reservations,tables-rooms,lookup-reservation}/*` — *new*.
- `src/app/features/reservations/routes.ts` — *new*.
- `src/app/app.routes.ts` — *modified*: registered the `reservations` feature.
- `src/app/layout/app/components/navigation/navigation.ts` — *modified*: added the "Reservations" nav group.
- `public/i18n/reservations/{en,fr,ar}.json` — *new*.
- `public/i18n/{en,fr,ar}.json` — *modified*: added the `navigation.reservations` key.

**Explicitly out of scope / not touched:** `menu-management`'s frontend (`features/menu/`),
`delivery-management`'s frontend (none exists), any manager-alert/notification system (removed
after being added — see the note in §3).
