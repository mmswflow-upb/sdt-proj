# Campus Room Reservation System

A microservices-based room reservation system built for milestone 4.

**Team Members:**
- Sakka Mohamad-Mario
- Zafar Azzam
- Al-Khalidy Essam

## What's This About?

We built a campus room reservation system where students can book rooms, faculty admins can manage those bookings, and everything is split into multiple services that talk to each other. The whole thing runs on Docker, so you don't need to worry about installing Java or databases locally.

## The Services

The system is made up of four main parts:

**Gateway Service** - The front door for everything. Routes requests to the right service and handles authentication. Runs on port 8080.

**Faculty Service** - Takes care of user accounts (students, professors, admins), faculties, rooms, and policies. Also handles login/registration and generates JWT tokens. When rooms are deleted or locked, it communicates with the other services to cascade those changes. Runs on port 8083.

**Reservation Service** - Manages room reservations. Students create them, admins approve or revoke them. Before creating a reservation, it talks to the scheduling service to verify room availability. Runs on port 8081.

**Scheduling Service** - Keeps track of room schedules and availability. Makes sure nobody double-books a room. Other services call it to check availability and manage schedules. Runs on port 8082.

Each service has its own PostgreSQL database, so they're completely independent. They communicate with each other using REST APIs - for example, when you try to reserve a room, the reservation service calls the scheduling service to check if it's free.

## Inter-Service Communication

The services talk to each other in a few key scenarios:

- **Reservation → Scheduling**: When creating a reservation, checks room availability
- **Faculty → Scheduling**: When deleting/locking a room, removes associated schedules
- **Faculty → Reservation**: When deleting/locking a room, revokes associated reservations

All communication happens via HTTP REST calls, and each service validates JWT tokens independently for security.

## Setup and Running

### Prerequisites

- **Docker Desktop** (Windows/Mac) or **Docker + Docker Compose** (Linux)
- **Postman** (for testing the API)

### Step 1: Start the Services

Clone the repo and navigate to the project directory, then run:

```bash
docker-compose up --build
```

This command will:

- Build all four microservices (gateway, faculty, reservation, scheduling)
- Spin up three PostgreSQL databases
- Start everything on their respective ports

Give it about a minute to fully start up. You'll know it's ready when you see logs from all services.

### Step 2: Verify Services Are Running

The services will be available on these ports:

- **Gateway Service**: `http://localhost:8080` (main entry point)
- **Faculty Service**: `http://localhost:8083`
- **Reservation Service**: `http://localhost:8081`
- **Scheduling Service**: `http://localhost:8082`

All API requests should go through the Gateway at port 8080.

### Step 3: Import Postman Collections

We've included comprehensive Postman collections split into separate workflow files (matching your K6 test structure):

1. Open Postman
2. Click **Import** in the top left
3. Select all `.json` files from the `postman-collections/` folder (or import individually):
   - **01-Setup.postman_collection.json** - Run this FIRST (registers users, creates resources)
   - **02-Student-Workflow.postman_collection.json** - Student operations
   - **03-Admin-Workflow.postman_collection.json** - Admin operations  
   - **04-Edge-Cases.postman_collection.json** - Error handling tests
   - **Authorization-Tests.postman_collection.json** - Role-based access control
   - **Campus-Reservation-Main.postman_collection.json** - All-in-one (if you prefer)

Each collection includes:

- **Automatic variable management** - JWT tokens and IDs are saved automatically
- **Test assertions** - Validates responses and saves values
- **Expected results** - Some tests should fail (edge cases like 409, 403, 404)
- **Sequential execution** - Run entire collection or individual requests

**Quick Start:**
1. Run **01-Setup** collection to initialize (registers users, creates faculty/room)
2. Run other collections in any order to test different workflows
3. Variables (tokens, IDs) flow automatically between requests

See `postman-collections/README.md` for detailed usage instructions.

### Step 4: Test the System

Start with the **Authentication** folder in Postman:

1. Register an admin account
2. Register a student account
3. Create a faculty
4. Create a room
5. Make a reservation as a student
6. Approve it as an admin

The collection is organized to follow realistic workflows, so going top to bottom works well.

### Stopping the Services

To stop everything:

```bash
docker-compose down
```

To stop and remove all data (databases):

```bash
docker-compose down -v
```

## Troubleshooting

**Services won't start**: Make sure ports 8080-8083 and 5433-5435 aren't being used by other applications.

**Connection errors between services**: Wait a full minute after running `docker-compose up`. The databases need time to initialize before the services can connect.

**Authentication fails**: Make sure you're using the token returned from login/register in the Authorization header as `Bearer <token>`.

## Testing

We've got K6 load tests in the `k6-tests` folder that simulate different user workflows - students making reservations, admins approving them, edge cases, etc. You can run them with `npm test` from that directory.

## Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Docker & Docker Compose
- JWT for authentication
- K6 for load testing

## How It Works

When a student wants to reserve a room, they send a request through the gateway. The faculty service checks if they're allowed to, the scheduling service checks if the room is free, and the reservation service creates the booking. Admins can then approve or deny those reservations. If someone deletes or locks a room, the system automatically cancels any reservations tied to it.

Everything talks to each other via REST APIs, and we use JWT tokens to make sure people can only do what they're supposed to.

---

That's pretty much it. Check the other markdown files in the repo if you need more details on specific workflows or testing scenarios.
