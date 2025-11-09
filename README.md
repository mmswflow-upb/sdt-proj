# Campus Reservations — Milestone 2: Classes & Relationships

---

## Team

- Sakka Mohamad-Mario 1241EB
- Zafar Azzam 1241EB
- Al-Khalidy Essam 1241EB

## Layered Overview

- **L1 — Application**: Commands and the command bus (use-cases).
- **L2 — Services**: Core orchestration and scheduling.
- **L3 — Validation (CoR)**: Pluggable, ordered validators.
- **L4 — Strategy**: Slot/room selection algorithm(s).
- **L5 — Eventing (Observer)**: Domain events and event bus implementations.
- **L6 — Domain**: Entities and enums.
- **L7 — Infrastructure**: Database abstraction and concrete connections.

Singletons: **CommandBus**, **InMemoryEventBus**, **MqttEventBus**, **InMemoryDatabaseConnection**, **SqlDatabaseConnection**.  
Enum-driven factories: **EventBusFactory(EventBusKind)**, **DatabaseFactory(DatabaseKind)**.

---

## L1 — Application (Commands)

### `Command` (interface)
- **Responsibility:** Uniform contract for application actions.
- **Key Operation:** `execute(ReservationService service)`.
- **Relations:** Implemented by concrete commands; executed by `CommandBus`.

### `CreateReservationCommand` (implements `Command`)
- **Responsibility:** Encapsulates data/intent to create a reservation request.
- **Holds:** `ReservationRequest req`, `User actor`.
- **Calls:** `ReservationService.submit(req, actor)`.
- **Relations:** Uses `ReservationService` via `CommandBus`.

### `ApproveReservationCommand` (implements `Command`)
- **Responsibility:** Approves a reservation request.
- **Holds:** `String requestId`, `User actor`.
- **Calls:** `ReservationService.approve(requestId, actor)`.
- **Relations:** Uses `ReservationService` via `CommandBus`.

### `CommandBus` *(Singleton)*
- **Responsibility:** Dispatches `Command` objects synchronously.
- **Holds:** `ReservationService service`.
- **Operations:** `getInstance(ReservationService)`, `dispatch(Command)`.
- **Relations:** Uses `ReservationService`; receives `Command`.

---

## L2 — Services

### `ReservationService`
- **Responsibility:** Orchestrates lifecycle: **validate → persist → schedule → publish events**.
- **Holds:**  
  - `DatabaseConnection db`  
  - `EventBus eventBus`  
  - `ValidationHandler validators` (head of CoR)  
  - `SchedulingService scheduling`  
  - `FacultyPolicy policy`
- **Operations:** `submit(ReservationRequest, User) : ValidationResult`, `approve(String, User)`.
- **Relations:**  
  - Composes/uses `ValidationHandler` chain (`FacultyPolicyValidator → ScheduleConflictValidator`).  
  - Uses `DatabaseConnection` for persistence.  
  - Uses `SchedulingService` for availability & blocking.  
  - Publishes `DomainEvent` via `EventBus`.  
  - Reads `FacultyPolicy` (policy constraints).

### `SchedulingService`
- **Responsibility:** Keeps occupancy, checks availability, blocks slots, proposes alternatives.
- **Holds:**  
  - `SlotSelectionStrategy strategy`  
  - `Map<String, List<TimeSlot>> occupancy`  
  - `List<Room> catalog`
- **Operations:** `available(String, TimeSlot): boolean`, `block(String, TimeSlot)`, `suggest(ReservationRequest): Room`.
- **Relations:**  
  - Used by `ReservationService` and `ScheduleConflictValidator`.  
  - Delegates alternative choice to `SlotSelectionStrategy`.

---

## L3 — Validation (Chain of Responsibility)

### `ValidationHandler` (abstract)
- **Responsibility:** Base link in CoR; defines `then()` and `validate()`.
- **Holds:** `ValidationHandler next`.
- **Operations:**  
  - `then(ValidationHandler) : ValidationHandler`  
  - `validate(ReservationRequest, User) : ValidationResult`  
  - `check(ReservationRequest, User) : ValidationResult` *(protected, to implement)*
- **Relations:** Parent of concrete validators; chained in `ReservationService`.

### `FacultyPolicyValidator` (extends `ValidationHandler`)
- **Responsibility:** Enforces faculty policy (faculty match, allowed roles, max duration, blackout).
- **Holds:** `FacultyPolicy policy`.
- **check():** Validates actor faculty/role/time window vs. policy.
- **Relations:** First link in validation chain.

### `ScheduleConflictValidator` (extends `ValidationHandler`)
- **Responsibility:** Ensures no schedule conflicts; proposes an alternative room if needed.
- **Holds:** `SchedulingService scheduling`.
- **check():** `available(room, slot)` else `suggest(req)`; returns chosen `Room` in `ValidationResult`.
- **Relations:** Follows `FacultyPolicyValidator` in the chain.

### `ValidationResult`
- **Responsibility:** Immutable outcome of a validation step/chain.
- **Holds:** `boolean ok`, `String msg`, `Room chosen` *(optional)*.
- **Construction:** `ok()`, `ok(Room)`, `fail(String)`.
- **Relations:** Returned by validators and consumed by `ReservationService`.

---

## L4 — Strategy

### `SlotSelectionStrategy` (interface)
- **Responsibility:** Select a room among available candidates.
- **Operation:** `select(List<Room>, ReservationRequest) : Room`.
- **Relations:** Implemented by concrete strategies, used by `SchedulingService`.

### `LowestConflictStrategy` (implements `SlotSelectionStrategy`)
- **Responsibility:** Simple policy selecting a suitable available room.
- **Operation:** `select(...) : Room`.
- **Relations:** Injected into `SchedulingService`.

---

## L5 — Eventing (Observer)

### `DomainEventType` (enum)
- **Values:** `RESERVATION_REQUESTED`, `RESERVATION_APPROVED`, `RESERVATION_REJECTED`.

### `DomainEvent`
- **Responsibility:** Represents a published domain event.
- **Holds:** `DomainEventType type`, `String aggregateId`, `Object payload`.

### `EventListener` (interface)
- **Responsibility:** Event handler contract.
- **Operation:** `on(DomainEvent event)`.

### `EventBus` (interface)
- **Responsibility:** Publish–subscribe abstraction.
- **Operations:** `publish(DomainEvent)`, `subscribe(DomainEventType, EventListener)`.

### `InMemoryEventBus` *(Singleton, implements `EventBus`)*
- **Responsibility:** In-process pub–sub for events.
- **Holds:** `Map<DomainEventType, List<EventListener>> routes`.
- **Operations:** `getInstance()`, `publish(...)`, `subscribe(...)`.
- **Relations:** Used by `ReservationService` in in-memory deployments.

### `MqttClient`
- **Responsibility:** Minimal client abstraction for MQTT publish/subscribe (stubbed API).
- **Operations:** `publish(topic, payload)`, `subscribe(topic, handler)`.

### `MqttEventBus` *(Singleton, implements `EventBus`)*
- **Responsibility:** Event bus backed by `MqttClient`.
- **Holds:** `MqttClient client`, local handlers per event type.
- **Operations:** `getInstance(MqttClient)`, `publish(...)`, `subscribe(...)`.
- **Relations:** Used by `ReservationService` in MQTT mode.

### `EventBusKind` (enum)
- **Values:** `IN_MEMORY`, `MQTT`.

### `EventBusFactory`
- **Responsibility:** Enum-driven creation of `EventBus`.
- **Operation:** `create(EventBusKind, MqttClient) : EventBus`.
- **Relations:** Produces singletons (`InMemoryEventBus` or `MqttEventBus`).

---

## L6 — Domain

### `ReservationStatus` (enum)
- **Values:** `PENDING`, `APPROVED`, `REJECTED`.

### `Role` (enum)
- **Values:** `PROFESSOR`, `ADMIN`, `STAFF`.

### `TimeSlot`
- **Responsibility:** Immutable interval with overlap/minutes helpers.
- **Holds:** `LocalDateTime start`, `LocalDateTime end`.
- **Key Ops:** `minutes()`, `overlaps(TimeSlot) : boolean`.

### `Room`
- **Responsibility:** Room capacity/equipment metadata.
- **Holds:** `String roomId`, `int capacity`, `Set<String> equipment`.
- **Key Op:** `hasAll(List<String>) : boolean`.

### `User`
- **Responsibility:** Requesting/approving actor.
- **Holds:** `String userId`, `Role role`, `String facultyKey`.

### `FacultyPolicy`
- **Responsibility:** Faculty constraints for validations.
- **Holds:** `String facultyKey`, `int maxMinutes`, `boolean enforceBlackout`, `boolean hardPref`, `List<Role> allowedRoles`.

### `ReservationRequest`
- **Responsibility:** Aggregate for a reservation.
- **Holds:**  
  - `String requestId` *(generated)*  
  - `String requesterId`  
  - `String roomId` *(mutable if an alternative is chosen)*  
  - `TimeSlot slot`  
  - `int attendees`  
  - `List<String> equipment`  
  - `ReservationStatus status`
- **Relations:** Validated by CoR; persisted by `DatabaseConnection`; scheduled by `SchedulingService`; referenced in `DomainEvent`.

---

## L7 — Infrastructure

### `DatabaseConnection` (interface)
- **Responsibility:** Persistence abstraction for `ReservationRequest`.
- **Operations:** `save(ReservationRequest)`, `markApproved(String)`, `exists(String): boolean`, `findById(String): ReservationRequest`.

### `InMemoryDatabaseConnection` *(Singleton, implements `DatabaseConnection`)*
- **Responsibility:** In-process map-backed storage.
- **Holds:** `Map<String, ReservationRequest> store`.
- **Relations:** Used in memory-mode; accessed by `ReservationService`.

### `SqlDatabaseConnection` *(Singleton, implements `DatabaseConnection`)*
- **Responsibility:** Placeholder for external SQL persistence (pool provided).
- **Holds:** `Object pool`.

### `DatabaseKind` (enum)
- **Values:** `IN_MEMORY`, `SQL`.

### `DatabaseFactory`
- **Responsibility:** Enum-driven creation of `DatabaseConnection`.
- **Operation:** `create(DatabaseKind, Object pool) : DatabaseConnection`.
- **Relations:** Produces singletons (`InMemoryDatabaseConnection` or `SqlDatabaseConnection`).

---

## Relationships Summary (selected)

- **Commands → Services**:  
  `CreateReservationCommand.execute()` → `ReservationService.submit(...)`  
  `ApproveReservationCommand.execute()` → `ReservationService.approve(...)`  
  `CommandBus.dispatch(...)` → `Command.execute(service)`

- **ReservationService → Validation**:  
  Builds chain `FacultyPolicyValidator → ScheduleConflictValidator`; calls `validate(...)`.

- **ReservationService → Infra/Eventing/Scheduling**:  
  `db.save(...)`, `db.markApproved(...)`, `db.exists(...)`  
  `scheduling.available(...)`, `scheduling.block(...)`  
  `eventBus.publish(DomainEvent)`

- **SchedulingService → Strategy**:  
  `suggest(req)` delegates to `SlotSelectionStrategy.select(...)`.

- **Factories (enum-driven) → Singletons**:  
  `EventBusFactory.create(EventBusKind, MqttClient)` → `InMemoryEventBus.getInstance()` or `MqttEventBus.getInstance(client)`  
  `DatabaseFactory.create(DatabaseKind, pool)` → `InMemoryDatabaseConnection.getInstance()` or `SqlDatabaseConnection.getInstance(pool)`

- **Domain used throughout**:  
  `ReservationRequest`, `TimeSlot`, `Room`, `User`, `FacultyPolicy`, `ReservationStatus`, `Role` are referenced by services, validators, strategy, eventing, and infra.

---