# Campus Reservations — Milestone 1

**Team:** Sakka Mohamad‑Mario · Al‑Khalidy Essam · Zafar Azzam  
**Project:** Campus‑wide backend to request and approve **room/lab reservations** for courses, labs, exams, and events. Proposed techstack: **Express + TypeScript**, **Docker**, **3 microservices**, **MQTT** for events, and **CI/CD on GCP**. (later milestones)

## Design Patterns Employed:

### 1) Chain of Responsibility (CoR)

**Simple idea:** Imagine a paper form moving across several desks. Each person checks one rule: dates make sense, room fits the class, equipment is available, the requester has permission, etc. If any rule fails, the form is stamped “Rejected” and stops there. If every desk approves, the request passes.

**Why we like it:** Our university rules can change, and different faculties may add their own checks. With CoR, we plug in or reorder checks without touching one giant function. Each check lives in its own tiny class, which makes unit testing and debugging straightforward.

**Where we use it:** Validating **reservation requests** in the Reservations Service: time bounds → overlap check → capacity → equipment → policy → role permission.

**Benefits over simpler options:**  
- A single big `validate()` full of `if/else` grows messy and is hard to test.  
- CoR is **composable** (add/remove steps), **orderable** (policy first or last), and **short‑circuits** early on failures.

#### Comparison — Validation Approaches

| Approach | Problem Context | Why it fits | Simpler Alternative | Trade‑offs | Where we use it |
|---|---|---|---|---|---|
| **Chain of Responsibility** | Many independent checks with early stop | Modular steps, reorderable, testable | One big `if/else` validator | Slightly more classes to manage | Reservation validation pipeline |
| Decorator | Add behavior around a single object | Good for wrapping features like logging/caching | Inline wrappers | Not natural for sequential rule failures | Cross‑cutting concerns only |
| Strategy | Choose **one** algorithm among many | Good when exactly one algorithm runs | Switch/if selection | Doesn’t model multiple sequential checks | Conflict policy selection (elsewhere) |
| One big function | Quick and simple | Fast to start | — | Hard to change/test; long & error‑prone | Not recommended for validation |

---

### 2) Strategy — “pick a policy like a plugin”

**Simple idea:** Different faculties may prefer different scheduling policies (earliest slot, same‑building preference, minimize walking distance, keep cohorts in one wing). Strategy lets us swap **the algorithm** without rewriting the rest of the service.

**Why we like it:** We can A/B test and tune policies per semester or department. It avoids giant `switch` statements spread everywhere. Each policy becomes a small, focused class that’s easy to benchmark and test.

**Where we use it:** **Conflict detection & slot selection** (which available room/time to propose when there are options).

**Benefits over simpler options:**  
- Replaces scattered `if/else` branches with clean, plug‑in policies.  
- Easier to roll out **new policies** without touching existing ones.

#### Comparison — Policy Selection

| Approach | Problem Context | Why it fits | Simpler Alternative | Trade‑offs | Where we use it |
|---|---|---|---|---|---|
| **Strategy** | Multiple interchangeable algorithms | Swap at runtime/config; clean testing | `if/else` or `switch` | Slight boilerplate (interfaces/classes) | Slot selection & conflict resolution |
| Template Method | Same skeleton, a few varying steps | If algorithms share a strict template | Strategy | Less flexible when algorithms differ a lot | Not ideal here |
| Hard‑coded rules | One fixed policy | Minimal setup | — | Hard to change; code churn per semester | Not recommended |

---

### 3) Observer (Publish–Subscribe via MQTT) — “announce news, let listeners react”

**Simple idea:** When something happens (a reservation gets approved), the service **publishes an event**. Anyone interested (notifications, hallway signage, analytics) **subscribes** and reacts. The publisher doesn’t know or care who listens.

**Why we like it:** This **decouples** services. We don’t have to call five different services every time. If a consumer is down, MQTT can retain or replay messages (depending on QoS) and the system is more resilient.

**Where we use it:** Emitting domain events like `reservation.requested`, `reservation.approved`, `reservation.rejected`, `room.updated`. The Notifications service subscribes and sends emails/webhooks; future services (calendar sync, digital signage) can subscribe later **without changing the producer**.

**Benefits over simpler options:**  
- Avoids tight coupling and slow chains of REST calls.  
- Real‑time updates without heavy polling.

#### Comparison — Service‑to‑Service Communication

| Approach | Problem Context | Why it fits | Simpler Alternative | Trade‑offs | Where we use it |
|---|---|---|---|---|---|
| **Observer / Pub‑Sub (MQTT)** | Many consumers, loose coupling, async | Scales, decouples, real‑time | Direct REST calls | Operational overhead (broker), eventual consistency | Broadcasting reservation/room events |
| Direct REST calls | Few consumers, synchronous needs | Simple, request/reply | — | Tight coupling, cascading failures | Admin UI queries |
| Polling | Consumers fetch updates on schedule | Easy to add on legacy systems | Cron jobs | Latency, wasted load, stale data | Not preferred |
| Global Event Bus (shared singleton) | Centralized events in‑process | Quick for monoliths | In‑process observers | Doesn’t cross service boundaries | Not for microservices |

---

### 4) Command — “package an action with its data”

**Simple idea:** We wrap each user action as a **Command object**: `CreateReservation`, `ApproveReservation`, `CancelReservation`, `RescheduleReservation`. A command carries the **intent** plus all data it needs and can be **queued, retried, logged, and audited** consistently.

**Why we like it:** Commands make it easy to add **idempotency** (same command key won’t double‑book), **retries** on temporary failures, and **audit trails** (who did what, when). They also keep the application layer clean and uniform.

**Where we use it:** Reservation lifecycle operations and admin actions. Commands can be executed synchronously or sent to a work queue when needed.

**Benefits over simpler options:**  
- Standardizes how we run, log, retry, and secure actions.  
- Plays nicely with messaging and compensating actions.

#### Comparison — Handling Actions

| Approach | Problem Context | Why it fits | Simpler Alternative | Trade‑offs | Where we use it |
|---|---|---|---|---|---|
| **Command** | Many actions need retry/audit/idempotency | Consistent execution & logging | Call methods directly | Slightly more structure (classes/handlers) | Reservation lifecycle operations |
| Direct service calls | Straightforward single action | Quick to write | — | Harder to add retries/idempotency uniformly | Limited use |
| Event Sourcing | Full history = source of truth | Powerful auditing | DB change log | Higher complexity, rebuilds | Possible later |
| Saga only | Distributed transactions via steps | Orchestrates multi‑service ops | Ad‑hoc workflows | Needs events/commands anyway | Later milestones |

---

### 5) Factory Method (supporting) — “one door to create clients cleanly”

**Simple idea:** Instead of sprinkling `new MqttClient(...)` or `new Pool(...)` everywhere, we centralize creation behind small factories. Tests can swap real clients with fakes/mocks easily. Deployments can change providers via configuration.

**Why we like it:** Makes code **testable** and **portable** (local vs. cloud). It keeps infrastructure details out of business logic.

**Where we use it:** Creating DB pools, MQTT clients, mail/webhook adapters based on environment (dev/test/prod).

**Benefits over simpler options:**  
- Encourages **explicit dependencies** and clean seams for testing.  
- Avoids hidden singletons and reduces setup duplication.

#### Comparison — Object Creation

| Approach | Problem Context | Why it fits | Simpler Alternative | Trade‑offs | Where we use it |
|---|---|---|---|---|---|
| **Factory Method** | Need swappable/testing‑friendly clients | Centralized creation, easy mocking | `new` everywhere | Slightly more boilerplate | DB/MQTT/email/webhook clients |
| Service Locator | Global registry gives instances | Quick wiring | Global singletons | Hidden dependencies, hard to test | Not preferred |
| Abstract Factory | Families of related objects | Useful at scale | Multiple factories | Heavier abstraction | Maybe later if needed |
| `new` scattered | Fast to start | Minimal code | — | Tight coupling, hard to test/swap | Avoid |

---

## How they work together (one scenario)

1. A professor submits a **reservation request**.  
2. The request runs through the **Chain of Responsibility** validators (time, conflicts, capacity, equipment, policy, permission).  
3. If conflicts occur but alternatives exist, the **Strategy** picks the best slot/room according to current policy.  
4. Approving the request executes an **ApproveReservation Command** that logs intent, ensures idempotency, writes to the DB, and emits events.  
5. The service **publishes** `reservation.approved` on MQTT (**Observer / Pub‑Sub**). The Notifications service and others react independently.

---

## TL;DR (pattern → problem → payoff)

| Pattern | Solves | Payoff |
|---|---|---|
| **Chain of Responsibility** | Many ordered checks that may fail early | Modular rules, easy to reorder and test |
| **Strategy** | Swap scheduling/conflict policies | Clean plug‑in policies, no branch explosions |
| **Observer (MQTT)** | Decouple producers/consumers across services | Real‑time updates, scalable fan‑out |
| **Command** | Uniform action execution with retry/audit/idempotency | Safer operations, consistent logging |
| **Factory Method** | Testable creation of infra clients | Easier testing and provider changes |
