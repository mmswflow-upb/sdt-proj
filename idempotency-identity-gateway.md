# Campus Reservations – Idempotency, User Identity Propagation & API Gateway Design

This document summarizes design decisions and ideas we discussed for:

- **Idempotent commands** (especially for reservations)
- **Propagation of user identity** in inter-service calls
- Adding a **Spring Cloud Gateway** in front of the microservices

It is meant as a conceptual/architectural note you can reference in your report or README.

---

## 1. Idempotency Strategy

### 1.1. Why we care about idempotency

We want to avoid situations where:

- A user sends the **same logical request** multiple times (retries, double-click, network issues).
- The backend accidentally creates **duplicate reservations** or performs the same state change twice.

For example:

- User presses “Reserve room ENG-101 from 10:00–12:00”.
- Request times out; user retries.
- We want the system to treat this as **one** reservation, not two.

### 1.2. Two general approaches we discussed

We talked about two standard strategies for idempotency keys:

1. **Client-generated random keys** (e.g. UUID in `Idempotency-Key` header):
   - The client generates a random unique key per logical operation.
   - The server stores the key and result; if the same key comes again, it reuses the result.
   - Collisions are practically impossible with UUIDs, but this **requires the client to reuse the same key** on retries.

2. **Server-generated deterministic keys** (based on business fields):
   - The server computes the key from the meaningful fields of the request.
   - Example: `(userId, roomId, startTime, endTime)` for a reservation.
   - This does not rely on the client sending any special header.

We also noted that if the client *doesn’t* reuse its random UUID, pure header-based idempotency doesn’t help; the server then needs **business-level duplicate checks** anyway.

### 1.3. Chosen direction: deterministic, business-based keys

Given the domain, we decided a **business-based idempotency key** is a good fit.

#### For creating reservations

We treat the tuple:

```text
(userId, roomId, startTime, endTime)
```

as the **identity of a “create reservation” command**.

Implications:

- If the same user tries to reserve the **same room** in the **same time period** again:
  - We consider it the **same logical operation**, not a new one.
  - The backend **must not create a second reservation** for that slot.
- Implementation options:
  - Use a unique constraint on `(userId, roomId, startTime, endTime)`  
    ⇒ any duplicate insert fails and is translated to a “duplicate reservation” error.
  - Or explicitly check in code:
    - “Is there already a PENDING/APPROVED reservation for this user+room+timeslot?”
    - If yes, either:
      - return the existing reservation, or
      - return a `409 Conflict` / specific error.

Note: **equipment does not affect** the idempotency key, because in this design:

- A student **does not reserve by equipment directly**.
- They first use `GET` endpoints to **search rooms by equipment**, then pick a specific `roomId`.
- The reservation request itself is for a **concrete room**, not “any room with a projector”.

#### For approving / cancelling reservations

For status changes (e.g. approve/revoke), we concluded that it’s simpler and more natural to use:

```text
(reservationId, operationType)
```

as the idempotency key, instead of repeating room/time/user.

Examples:

- Approve command key: `"APPROVE:" + reservationId`
- Cancel/Revoke command key: `"CANCEL:" + reservationId`

This means:

- Multiple attempts to **approve the same reservation** are treated as the **same command**.
- The first successful attempt changes the status; subsequent attempts are idempotent (no-op or “already approved” response).

If we later add a `Command` table, each command could store:

- `commandKey` (unique) based on the above logic,
- `type` (`CREATE_RESERVATION`, `APPROVE_RESERVATION`, `CANCEL_RESERVATION`),
- `payload`, `status`, `resultReservationId`, timestamps, etc.

---

## 2. User Identity Propagation in Inter-Service Calls

### 2.1. Original approach: system token

Initially, the idea was:

- Each service validates user JWTs coming from the client.
- For **inter-service calls**, services would generate a separate **“system admin”** JWT and send that instead.
  - Example: `reservation-service` → `scheduling-service` uses a system token with role `ADMIN`.

Downsides:

- Downstream services don’t know which **human user** initiated the action.
- Harder to audit (“who locked this room?”).
- Authorization in downstream services is no longer user-based, but service/account-based.

### 2.2. Improved approach: propagate user identity

We discussed a better option for **synchronous user-driven flows**:

> **Forward the original user’s JWT** in inter-service calls.

In other words:

- Client → Faculty-service: `Authorization: Bearer userToken`
- Faculty-service → Reservation-service: **same** `Authorization: Bearer userToken`
- Faculty-service → Scheduling-service: **same** `userToken` if needed
- Reservation-service → Scheduling-service: also forwards `userToken`

Each service:

- Validates the JWT the same way (shared secret).
- Sees the **real `userId` and role**.
- Applies its own authorization rules accordingly.

Benefits:

- **True end-to-end identity**:
  - Every service knows exactly *which* user initiated the action.
- Better auditing:
  - Logs in all services can show `userId` and `role`.
- More flexible authorization:
  - Downstream can enforce user-level rules (e.g. who is allowed to modify certain schedules).

We also acknowledged:

- For **async/background jobs** (e.g. nightly cleanup, event consumers), there may be **no user**:
  - Those flows would still use a **system/service token** or run under a special internal identity.
- For **synchronous HTTP workflows**, forwarding the user token is preferred and clean.

---

## 3. Adding Spring Cloud Gateway

### 3.1. Motivation

We want to introduce an **HTTP API gateway** in front of the microservices to:

- Provide a **single entry point** for clients.
- Route requests to the right microservice based on the URL path.
- Keep **authentication and fine-grained authorization inside each microservice**.

Instead of clients calling:

- `localhost:8083` (faculty-service),
- `localhost:8081` (reservation-service),
- `localhost:8082` (scheduling-service),

they will call one gateway, e.g.:

- `localhost:8080/api/...`

### 3.2. Choice: Spring Cloud Gateway vs NGINX

We discussed two options:

- **NGINX** – great as a fast, low-level reverse proxy, but not Spring-specific.
- **Spring Cloud Gateway** – implemented as a Spring Boot app, fits better with our stack.

Given:

- The system is entirely Spring Boot + Java.
- We want something easy to integrate with existing code and config.
- This is an academic project (not extreme production scale).

We leaned towards:

> **Using Spring Cloud Gateway** as the HTTP gateway.

Advantages for this project:

- Same ecosystem (Spring Boot, `application.yml`, Java filters).
- Easier to reason about in documentation and diagrams.
- Straightforward to extend with custom routing and filter logic if needed.

### 3.3. Responsibilities of the gateway

In the design we want:

- The gateway **routes requests** based on URL patterns, e.g.:

  - `/api/auth/**` → faculty-service
  - `/api/faculties/**` and `/api/faculty-rooms/**` → faculty-service
  - `/api/reservations/**` → reservation-service
  - `/api/scheduling/**` and `/api/rooms/**` → scheduling-service

- The gateway:
  - **Forwards the `Authorization` header** unchanged.
  - May handle cross-cutting concerns like CORS, basic logging, rate limiting, etc.
- **Each microservice still**:
  - Validates the JWT (using `JwtAuthFilter` + shared secret).
  - Enforces its own authorization rules (roles, ownership, domain-specific checks).

For internal **service-to-service calls**:

- We keep them **direct** (bypassing the gateway) on the internal Docker network:
  - `faculty-service` → `reservation-service` / `scheduling-service`
  - `reservation-service` → `scheduling-service`
- These calls propagate the **same user JWT** for synchronous user-triggered flows.
- The gateway is the edge for *external* clients, not for internal RPC between microservices.

---

## 4. Summary

- **Idempotency key**:
  - For **create reservation**: use a deterministic key based on `(userId, roomId, startTime, endTime)`.
  - For **status changes** (approve/cancel): key based on `(reservationId, operationType)`.
  - This reflects the natural identity of commands without requiring a special client header.
- **Inter-service communication**:
  - For synchronous workflows, we want to **propagate the user’s JWT** between services instead of using a generic system token.
  - This gives end-to-end user identity and better authorization/audit behavior.
- **Spring Cloud Gateway**:
  - Add it as the **single HTTP entry point**.
  - Use it to route paths to the appropriate microservice.
  - Let each microservice still perform authentication and authorization internally.

This gives a clean, realistic microservice architecture that is easy to explain in an academic context and can be evolved further for future milestones.
