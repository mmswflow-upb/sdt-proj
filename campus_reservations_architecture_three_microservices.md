# Campus Reservations – Architecture Overview

## Team

- Sakka Mohamad-Mario 1241EB
- Zafar Azzam 1241EB
- Al-Khalidy Essam 1241EB

---

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

Pros:
- Simple to develop, run, and debug (one app, one DB).
- No network calls between internal components.
- Easy to understand for new team members.

Cons:
- We can only scale the whole application, not individual parts.
- As the codebase grows, deployments become more risky and slower.
- Technology choices are coupled across the whole system.

---

## 2. Microservices Architecture (Three Services)

In the microservices version we split the system into three services, each running in its own process and owning its own data:

1. Command + Reservation Service  
2. Scheduling Service  
3. Notification Service  

The services communicate over HTTP/gRPC and via an event bus.

### 2.1 Command + Reservation Service

This service combines the API endpoints, command handling, and reservation logic.

- Exposes HTTP/gRPC endpoints to clients.
- Hosts the `CommandBus` and the command classes.
- Contains `ReservationService`, validators, and domain entities related to reservations.
- Owns its own database:
  - Reservation records.
  - Optionally a log of executed commands for auditing.
- Publishes domain events such as `RESERVATION_REQUESTED`, `RESERVATION_APPROVED` to the event bus.

Example flow (Create Reservation):

1. Client calls `POST /reservations`.
2. Controller builds a `CreateReservationCommand` and sends it to `CommandBus`.
3. `ReservationService`:
   - Runs the validation chain.
   - Calls the Scheduling Service API to check availability or get suggestions.
   - Writes the reservation to its own Reservations DB.
   - Publishes a `RESERVATION_REQUESTED` event to the event bus.

### 2.2 Scheduling Service

This service owns room and timeslot logic and its own database.

- Keeps the catalog of rooms and their equipment.
- Keeps occupancy information (what is booked when).
- Exposes APIs such as:
  - `GET /availability?roomId=...&slot=...`
  - `POST /suggest` with a `ReservationRequest` payload.
- Owns a separate database with:
  - Room definitions.
  - Time slots and occupancy state.

Example usage:

- The Command + Reservation Service calls:
  - `GET /availability` during conflict checks.
  - `POST /suggest` when an alternative room is needed.

### 2.3 Notification Service

This service is fully event-driven and subscribes to domain events.

- Subscribes to events published to the event bus, for example:
  - `RESERVATION_REQUESTED`
  - `RESERVATION_APPROVED`
  - `RESERVATION_REJECTED`
- Sends emails or other notifications to users based on those events.
- Can maintain its own small database:
  - Notification templates.
  - Notification logs (what was sent and when).

Example flow (Notification):

1. Command + Reservation Service publishes `RESERVATION_APPROVED` to the event bus.
2. Notification Service is subscribed to this event type.
3. It receives the event payload, looks up the user and template, and sends the notification.
4. It optionally records the notification in its own DB.

### 2.4 Event Bus

- The event bus is usually backed by a message broker such as MQTT or another pub/sub system.
- The Command + Reservation Service publishes domain events.
- The Notification Service subscribes to those events.
- Other services (such as analytics) can also subscribe without changing the core services.

### Microservices Pros and Cons

Pros:
- Clear separation of concerns between command handling, scheduling, and notifications.
- Each service scales independently (for example, Scheduling and Notification can scale up during peak times).
- Good alignment with our domain boundaries: reservations, schedules, notifications.
- Easier to add new services later by subscribing to domain events.

Cons:
- Higher operational complexity (service discovery, monitoring, distributed logging, CI/CD per service).
- Requires stable and well-defined APIs and message formats between services.
- Cross-service workflows need patterns like retries, sagas, and idempotent operations.

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
- Notification functions
  - Trigger: messages on an event topic.
  - React to `RESERVATION_REQUESTED` or `RESERVATION_APPROVED` and send notifications.

Shared entities, validators, and database helpers are extracted into a common library or runtime layer that all functions reuse.

### Why It Is Event-Driven

- Important changes are modelled as domain events such as `RESERVATION_REQUESTED` and `RESERVATION_APPROVED`.
- Notification and other functions subscribe to these events.
- Functions are triggered by:
  - HTTP events from clients,
  - messages on the event bus,
  - scheduled timers for clean-ups and periodic jobs.

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
- Microservices (three services)
  - Command + Reservation, Scheduling, Notification services reflect clear domain boundaries.
  - Supports independent scaling, deployment, and ownership of each area.
  - Works naturally with an event bus and domain events, especially for notifications.
- Event-Driven Serverless
  - Minimal infrastructure management and naturally event-driven.
  - Better suited for auxiliary tasks than for the core multi-step reservation flow in our case.

Final decision:

For Campus Reservations, our preferred long-term architecture is the microservices approach with three services: Command + Reservation Service, Scheduling Service, and Notification Service. The current design already matches these boundaries, and turning them into separate services with their own databases and APIs gives us independent scaling, clearer responsibilities, and room to add new features as separate services.

In the short term, we can still start from a modular monolith, keeping these boundaries inside one codebase and one deployment. Serverless functions are best used for supporting tasks such as notifications, periodic clean-ups, and exports, where an event-driven, on-demand execution model fits well, while the main booking flow lives in the microservices architecture.
