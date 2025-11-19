# Campus Reservations – Architecture Evaluation

## 1. Monolithic Architecture

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
  - Create `DatabaseConnection` and `EventBus` using the factories.
  - Build `SchedulingService`, the validation chain, and `ReservationService`.
  - Create a `CommandBus` that holds the `ReservationService` and register HTTP endpoints.
- A typical HTTP request builds a `CreateReservationCommand` or `ApproveReservationCommand`, calls `CommandBus.dispatch(command)` in the same process, and returns the result from `ReservationService`.

**Pros:**

- Simple to develop, run, and debug (one app, one DB).
- No network calls between internal components.
- Easy to understand for new team members.

**Cons:**

- We can only scale the whole application, not individual parts.
- As the codebase grows, deployments become more risky and slower.
- Technology choices are coupled across the whole system.

---

## 2. Microservices Architecture

Here we split the system into three services. Each service runs in its own process and has its own database or schema.

- Command + Reservation Service
  - Exposes HTTP/gRPC endpoints to clients.
  - Hosts the `CommandBus` and the command classes.
  - Contains the `ReservationService`, validation chain, and domain logic for reservations.
  - Owns a **Commands + Reservations DB**, where it:
    - Stores incoming commands with an idempotency key and status (`PENDING`, `APPROVED`, `REJECTED`/`REVOKED`).
    - Stores the corresponding reservation records.
  - Publishes domain events such as `RESERVATION_REQUESTED` and `RESERVATION_APPROVED` to a message queue (for example MQTT topic or another broker).

- Scheduling Service
  - Owns room, occupancy, and timeslot logic.
  - Exposes APIs like `available(roomId, slot)` and `suggest(request)`.
  - Has its own **Scheduling DB** with:
    - Room definitions and equipment.
    - Occupancy information (blocked time slots).

- Notification Service
  - Subscribes to domain events from the message queue (for example `RESERVATION_REQUESTED`, `RESERVATION_APPROVED`, `RESERVATION_REJECTED`).
  - Sends emails or other notifications based on these events.
  - Can store sent notifications, templates, or user preferences in its own **Notifications DB**.

### Example Microservices Implementation

- Each service is packaged and deployed separately (for example as containers).

- For a create reservation flow:
  - The Command + Reservation Service receives `POST /reservations`.
  - It writes a new command entry in the Commands table with an idempotency key and status `PENDING`.
  - It checks if a command with the same key was already processed and, if yes, returns the stored result (idempotent behaviour).
  - It runs the validation chain and calls the Scheduling Service API to check conflicts or get a suggestion.
  - It writes/updates the reservation record in the Reservations table and sets the command status to `APPROVED` or `REJECTED`.
  - It publishes a `RESERVATION_REQUESTED` or `RESERVATION_APPROVED` event to the message queue.

- The Scheduling Service:
  - Exposes endpoints such as `GET /availability` and `POST /suggest`.
  - Reads/writes from its own Scheduling DB (rooms + occupancy).
  - Is called synchronously by the Command + Reservation Service during validation/scheduling.

- The Notification Service:
  - Subscribes to the relevant topics/queues on the message broker.
  - When it receives a `RESERVATION_APPROVED` event, it looks up notification settings/templates, sends the email, and logs the notification to the Notifications DB.
  - New consumers (for example an Analytics Service) can be added later by also subscribing to the same events without modifying the Command + Reservation or Scheduling services.

- Each core service (Command + Reservation, Scheduling, Notification) has its own configuration, scaling settings, and release pipeline.

**Pros:**

- Clear separation of concerns between:
  - Command + Reservation (entrypoints, idempotency, reservation lifecycle),
  - Scheduling (rooms and occupancy),
  - Notifications (side effects based on domain events).
- The Commands + Reservations DB supports idempotent processing and auditing of commands.
- Each service can be scaled, updated, and deployed independently.
- Notifications are loosely coupled: they react to events in a message queue rather than being called directly.
- Fault isolation: failure in the Notification Service does not block the main reservation flow; events can be retried from the queue.

**Cons:**

- Higher operational complexity (service discovery, monitoring, distributed logging, CI/CD per service, message broker).
- Requires well-defined API contracts between Command + Reservation and Scheduling, and clear event formats on the queue.
- More moving parts to coordinate when changing cross-service flows.

---

## 3. Event-Driven Serverless Architecture

In the serverless variant we implement the main flows as cloud functions. The cloud provider manages runtime and scaling, and we model important changes as domain events.

- `CreateReservation` function
  - Trigger: HTTP request when a student submits a form.
  - Steps: validate the request using shared validators, call a scheduling function, write to a cloud database, then publish a `RESERVATION_REQUESTED` event.
- `ApproveReservation` function
  - Trigger: HTTP request when an approver acts.
  - Steps: load and update the reservation, write to the DB, publish `RESERVATION_APPROVED`.
- `SchedulingFunction`
  - Trigger: direct invocation from other functions.
  - Contains the logic from `SchedulingService` for availability checks and suggestions.
- Notification and analytics functions
  - Trigger: messages for `RESERVATION_REQUESTED`, `RESERVATION_APPROVED`, and similar event types.
  - React to events and send emails or record metrics.

Shared entities, validators, and database helpers are extracted into a common library or runtime layer that all functions reuse. Functions are triggered by HTTP events, message-queue events, or scheduled timers, and they react to domain events instead of calling each other directly.

**Pros:**

- No server or process management; the cloud provider handles provisioning and scaling.
- Good for spiky or occasional workloads and background tasks.
- Easy to add new behaviours by subscribing new functions to existing events.

**Cons:**

- The main booking flow is multi-step and stateful, which is harder to follow when split across many functions.
- Stronger coupling to a specific cloud provider and its limits.
- Cold starts and resource limits can impact response times for interactive users.

---

## 4. Comparison and Final Decision

### Summary

- Monolithic
  - Simple, one deployment, ideal for early development and for teaching.
  - Limited scaling per component and less flexibility as the system grows.

- Microservices
  - Splits the system into:
    - Command + Reservation Service (with Commands + Reservations DB and idempotent command handling),
    - Scheduling Service (with Scheduling DB),
    - Notification Service (listening on a message queue).
  - Fits our logical boundaries and supports independent scaling and deployment of each core concern.
  - Works well with our existing event concepts and allows new services (extra notifications, analytics, reporting) to be added by subscribing to the same events on the message queue.
  - Requires more infrastructure and operational tooling, but there is a clear evolution path from the current modular design.

- Event-Driven Serverless
  - Minimal infrastructure management and naturally event-driven.
  - Better suited for auxiliary tasks than for the core multi-step reservation flow in our case.

### Final decision

For Campus Reservations, our preferred long-term architecture is the microservices approach. The current codebase already has clear boundaries that map well to a Command + Reservation Service (with idempotent command handling and its own database), a Scheduling Service (owning room and schedule data), and a Notification Service (subscribing to events from a message queue). Turning these into separate services gives us independent scaling, clearer ownership, and room to add new services such as extended notifications or analytics by subscribing to the same domain events.
