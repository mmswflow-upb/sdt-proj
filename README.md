# Campus Room Reservation System

A microservices-based room reservation system built for milestone 4.

## The Implementation

The system is made up of four main parts:

**Gateway Service** - The front door for everything. Routes requests to the right service and handles authentication. Runs on port 8080.

**Faculty Service** - Takes care of user accounts (students, professors, admins), faculties, rooms, and policies. Also handles login/registration and generates JWT tokens. When rooms are deleted, it communicates with the other services to cascade those changes. Runs on port 8083.

**Reservation Service** - Manages room reservations. Students create them, admins approve or revoke them. Before creating a reservation, it talks to the scheduling service to verify room availability. Runs on port 8081.

**Scheduling Service** - Keeps track of room schedules and availability. Makes sure nobody double-books a room. Other services call it to check availability and manage schedules. Runs on port 8082.

Each service has its own PostgreSQL database, so they're completely independent. They communicate with each other using REST APIs - for example, when you try to reserve a room, the reservation service calls the scheduling service to check if it's free.

## Inter-Service Communication

The services talk to each other in a few key scenarios:

- **Reservation -> Scheduling**: When creating a reservation, checks room availability
- **Faculty -> Scheduling**: When deleting a room, removes associated schedules
- **Faculty -> Reservation**: When deleting a room, revokes associated reservations

All communication happens via HTTP REST calls, and each service validates JWT tokens independently for security.

## Setup and Running

### Prerequisites

- **Docker Desktop** (Windows/Mac) or **Docker + Docker Compose** (Linux)
- **Postman** (for testing the API)

### Step 1: Start the Services

Clone the repo and navigate to the project directory, then run:

```bash
docker compose up --build
```

This command will:

- Build all four microservices (gateway, faculty, reservation, scheduling)
- Spin up three PostgreSQL databases
- Start everything on their respective ports

Give it about a minute to fully start up. You'll know it's ready when you see logs from all services.

### Step 2: Verify Services Are Running

Verify if images were created, you should see 4 images starting with `sdt-campus-*` and a `postgres 15` image:

```bash
docker images
```

Verify if containers are running:

```bash
docker ps
```

### Step 3: Import Postman Collections

Navigate to the `postman-collections` folder in this repository. You'll find:

- **`SDT-Campus-Reservation.postman_environment.json`** - The shared environment file with all configuration and variables
- **`1-Setup.postman_collection.json`** - Initial setup (login admin, create faculty, rooms, policy, users)
- **`2-Student-Workflow.postman_collection.json`** - Student reservation operations
- **`3-Admin-Workflow.postman_collection.json`** - Admin and faculty admin operations
- **`4-Edge-Cases.postman_collection.json`** - Edge cases and conflict scenarios
- **`5-Authorization-Tests.postman_collection.json`** - Role-based access control tests

**Import Process:**

1. Open Postman
2. Click **Import** (top left)
3. Select all 6 files from the `postman-collections` folder
4. After importing, select the **SDT-Campus-Reservations** environment from the environment dropdown (top right)

**Running the Collections:**

Run the collections **in this exact order**:

1. **1-Setup** - Creates the foundation (admin login, faculty, rooms, policy, test users)
2. **2-Student-Workflow** - Tests student reservation flows (create, view, concurrent bookings)
3. **3-Admin-Workflow** - Tests admin operations (approve, revoke, room management)
4. **4-Edge-Cases** - Tests conflict resolution (duplicate bookings, overlapping times, cancellations)
5. **5-Authorization-Tests** - Verifies role-based access control

For detailed information about each collection's structure and workflow, [see here](postman-collections/README-Postman.md`).

### Stopping the Services

To stop everything:

```bash
docker compose down
```

To stop and remove all volumes

```bash
docker compose down -v
```

To stop and remove volumes and images

```bash
docker compose down -v --rmi all
```

## Troubleshooting

**Services won't start**: Make sure ports 8080-8083 and 5433-5435 aren't being used by other applications.

**Connection errors between services**: Wait a full minute after running `docker compose up`. The databases need time to initialize before the services can connect.

**Authentication fails**: Make sure you're using the token returned from login/register in the Authorization header as `Bearer <token>`.

## Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Docker & Docker Compose
- JWT for authentication
- Postman for testing
