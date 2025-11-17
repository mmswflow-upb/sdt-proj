# Campus Reservations – Architecture Evaluation


## 1. Monolithic Architecture (Modular Monolith)

In the monolithic version we run a single application and a single database. We still keep a clear internal structure:

- Application layer: commands and `CommandBus` (use-cases).
- Services: `ReservationService`, `SchedulingService`.
- Validation: chain of responsibility (`FacultyPolicyValidator`, `ScheduleConflictValidator`).
- Strategy: `SlotSelectionStrategy` / `LowestConflictStrategy`.
- Eventing: `EventBus` with `InMemoryEventBus` or `MqttEventBus`.
- Domain: `ReservationRequest`, `User`, `Room`, `TimeSlot`, enums.
- Infrastructure: `DatabaseConnection` with in-memory or SQL implementations.

### Example Monolith Implementation

- Main entry point: `CampusReservationsApplication` (single deployable).
- On startup we:
  - Create a `DatabaseConnection` using `DatabaseFactory` (in-memory or SQL).
  - Create an `EventBus` using `EventBusFactory` (in-memory or MQTT).
  - Build a `SchedulingService` with a `SlotSelectionStrategy`.
  - Build the validation chain: `FacultyPolicyValidator -> ScheduleConflictValidator`.
  - Create a `ReservationService` that uses the DB, event bus, validators, and scheduling.
  - Create a `CommandBus` that holds the `ReservationService`.
  - Register HTTP endpoints (or CLI commands) that translate requests into commands.
- A typical HTTP request:
  - Controller builds a `CreateReservationCommand` or `ApproveReservationCommand`.
  - Calls `CommandBus.dispatch(command)` in the same process.
  - Returns the result from `ReservationService` directly.

**Pros**:
- Simple to develop, run, and debug (one app, one DB).
- No network calls between internal components.
- Easy to understand for new team members.

**Cons**:
- We can only scale the whole application, not individual parts.
- As the codebase grows, deployments become more risky and slower.
- Technology choices are coupled across the whole system.

---

## 2. Microservices Architecture

Here we split the system along the existing boundaries we already have in code. Each service runs in its own process and has its own database or schema.

- API / Command Service
  - Exposes HTTP/gRPC endpoints to clients.
  - Hosts the `CommandBus` and the command classes.
  - Translates incoming requests into calls to other services.
- Reservation Service
  - Owns reservation validation and domain types.
  - Has its own reservations database (schema optimized for reservations).
  - Publishes domain events such as `RESERVATION_REQUESTED` and `RESERVATION_APPROVED`.
- Scheduling Service
  - Owns room, occupancy, and timeslot logic.
  - Exposes APIs like `available(roomId, slot)` and `suggest(request)`.
- Event Bus Service
  - Wraps the message broker (for example MQTT) behind a simple interface.
  - Delivers domain events between services and external consumers.

### Example Microservices Implementation

- Each service is packaged and deployed separately (for example as containers).
- The API / Command Service:
  - Receives a HTTP call for creating a reservation.
  - Calls the Reservation Service over HTTP/gRPC.
- The Reservation Service:
  - Validates using its own validation chain and `SchedulingService` API for conflicts.
  - Writes to its own reservations database.
  - Publishes `RESERVATION_REQUESTED` to the Event Bus Service.
- Other services (notifications, analytics) subscribe to these events via the Event Bus Service.
- Each service has its own configuration, scaling settings, and release pipeline.

Pros:
- Matches our existing code boundaries (Reservation, Scheduling, EventBus).
- Each service can be scaled, updated, and deployed independently.
- Easier to add new capabilities (notifications, analytics, reporting) by subscribing to existing events.
- Fault isolation: a problem in one service does not necessarily stop the whole system.

Cons:
- Higher operational complexity (service discovery, monitoring, distributed logging, CI/CD per service).
- Cross-service workflows require patterns such as sagas, retries, and idempotent operations.
- Requires a bit more coordination between services and clear API contracts.

---

## 3. Event-Driven Serverless Architecture

In the serverless variant we implement the main flows as cloud functions and use events to connect them. The cloud provider manages the runtime and scaling.

### Example Functions

- CreateReservation function
  - Trigger: HTTP request when a student submits a form.
  - Steps: validate the request, call a scheduling function, write to a cloud database, publish `RESERVATION_REQUESTED`.
- ApproveReservation function
  - Trigger: HTTP request when an approver acts.
  - Steps: check that the request exists, update status, publish `RESERVATION_APPROVED`.
- SchedulingFunction
  - Trigger: direct invocation from other functions or an event.
  - Contains the logic from `SchedulingService` for availability checks and suggestions.

Shared entities, validators, and database helpers are extracted into a common library or runtime layer that all functions reuse.

### Why It Is Event-Driven

- Important changes are modelled as domain events such as `RESERVATION_REQUESTED` and `RESERVATION_APPROVED`.
- Other functions subscribe to these events, for example:
  - SendNotificationOnReservationRequested
  - LogAnalyticsOnReservationApproved
- Functions are triggered by:
  - HTTP events from clients,
  - messages on the event bus,
  - scheduled timers for clean-ups and periodic jobs.

### Example Serverless Implementation

- Each function is defined with its trigger in the cloud platform configuration.
- A new reservation:
  - HTTP trigger calls `CreateReservation`.
  - `CreateReservation` uses the shared library to validate and write to the DB.
  - It publishes a message to an event topic.
- A notification function:
  - Subscribes to the topic for `RESERVATION_REQUESTED`.
  - Sends an email or push notification when it receives the event.
- Logging and metrics are collected from function executions by the cloud platform.

Pros:
- No server or process management; the cloud provider handles provisioning and scaling.
- Good for spiky or occasional workloads and background tasks.
- Easy to add new behaviours by subscribing new functions to existing events.

Cons (for our use case):
- The main booking flow is multi-step and stateful, which is harder to follow when split across many functions.
- Stronger coupling to a specific cloud provider and its limits.
- Cold starts and resource limits can impact response times for interactive users.

---

## 4. Comparison and Final Decision

Summary:

- Monolithic (modular)
  - Simple, one deployment, ideal for early development and for teaching.
  - Limited scaling per component and less flexibility as the system grows.
- Microservices
  - Fits our existing logical boundaries and supports independent scaling and deployment.
  - Works well with our existing event concepts and allows new services to be added around the core.
  - Requires more infrastructure, but there is a clear evolution path from the current modular design.
- Event-Driven Serverless
  - Minimal infrastructure management and naturally event-driven.
  - Better suited for auxiliary tasks than for the core multi-step reservation flow in our case.

Final decision:

For Campus Reservations, our preferred long-term architecture is the microservices approach. The current codebase already has clear boundaries (Reservation, Scheduling, Event Bus), which can be turned into separate services with well-defined APIs and their own data stores. This gives us independent scaling, clearer ownership, and room to add new services such as notifications or analytics by subscribing to domain events.

In the short term, we can still start from a modular monolith implementation, since it is easier to develop and test. The monolith can be used as a stepping stone, keeping the same logical layers and boundaries. Serverless functions are best used for supporting tasks such as notifications, periodic clean-ups, and exports, where an event-driven, on-demand execution model fits well, while the main booking flow lives in the microservices architecture.
