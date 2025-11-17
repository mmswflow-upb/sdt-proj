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

- We have a main class, for example `CampusReservationsApplication`.
- On startup we:
  - Create a `DatabaseConnection` using `DatabaseFactory` (in-memory or SQL).
  - Create an `EventBus` using `EventBusFactory` (in-memory or MQTT).
  - Build a `SchedulingService` with a `SlotSelectionStrategy`.
  - Build the validation chain: `FacultyPolicyValidator -> ScheduleConflictValidator`.
  - Create a `ReservationService` that uses the DB, event bus, validators, and scheduling.
  - Create a `CommandBus` that holds the `ReservationService`.
- HTTP endpoints (or a CLI) construct commands such as `CreateReservationCommand` and `ApproveReservationCommand` and call `CommandBus.dispatch(command)` inside the same process.

Pros:
- Simple to develop, run, and debug.
- No network calls between internal components.
- Good fit for our current project size.

Cons:
- We can only scale the whole application, not individual parts.
- As the codebase grows, deployments become more risky.

---

## 2. Microservices Architecture

Here we split the system along the existing boundaries we already have in code:

- API / Command Service
  - Exposes HTTP/gRPC endpoints.
  - Hosts the `CommandBus` and the command classes.
- Reservation Service
  - Owns reservation validation and domain types.
  - Has its own Reservations database.
  - Publishes domain events such as `RESERVATION_REQUESTED`, `RESERVATION_APPROVED`.
- Scheduling Service
  - Owns room and timeslot logic.
  - Exposes APIs like `available(roomId, slot)` and `suggest(request)`.
- Event Bus Service
  - Wraps the broker (for example MQTT) behind a simple interface.

Pros:
- Matches our code boundaries (Reservation, Scheduling, EventBus).
- Each service can scale and be deployed independently.
- New features (notifications, analytics) can be added by subscribing to events.

Cons:
- Higher operational complexity (service discovery, monitoring, CI/CD per service).
- Cross-service workflows require patterns such as sagas or idempotent operations.

---

## 3. Event-Driven Serverless Architecture

In the serverless variant we implement the main flows as cloud functions and use events to connect them.

### Example Functions

- CreateReservation function
  - Trigger: HTTP request when a student submits a form.
  - Steps: validate request, call scheduling, write to a cloud database, publish `RESERVATION_REQUESTED`.
- ApproveReservation function
  - Trigger: HTTP request when an approver acts.
  - Steps: check that the request exists, update status, publish `RESERVATION_APPROVED`.
- SchedulingFunction
  - Trigger: direct call or event.
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
  - or scheduled timers for clean-ups.

Pros:
- No server management; the cloud provider handles scaling.
- Good for spiky or occasional workloads and background tasks.
- Easy to add new behaviours by subscribing to existing events.

Cons (for our use case):
- The main booking flow is multi-step and stateful, which is harder to follow when split across many functions.
- Stronger coupling to a specific cloud provider.
- Cold starts and resource limits can impact response times.

---

## 4. Comparison and Final Decision

Summary:

- Monolithic (modular)
  - Simple, one deployment, ideal for our current project and for teaching.
  - Limited scaling per component as the system grows.
- Microservices
  - Fits our existing boundaries and allows independent scaling and deployment.
  - Requires more DevOps effort and operational tooling.
- Event-Driven Serverless
  - Minimal infrastructure management and naturally event-driven.
  - Better suited for auxiliary tasks than for our core multi-step reservation flow.

Final decision:

For Campus Reservations, we would implement the main booking flow as a modular monolith, using the layered design from Milestone 2. If the system grows in usage and complexity, we would then consider splitting it into microservices along the Reservation, Scheduling, and Event Bus boundaries. Serverless functions make the most sense for supporting tasks such as notifications, periodic clean-ups, and exports, where an event-driven, on-demand execution model fits well.

