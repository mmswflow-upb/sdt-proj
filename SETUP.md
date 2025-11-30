# Campus Reservations – Microservices (Java 21)

This project is the **microservices implementation** of the Campus Reservations system.
It is split into three independent Spring Boot services, each using **Java 21** and
**PostgreSQL**, and orchestrated with **Docker Compose**.

> On Windows, the easiest way to run everything is with **Docker Desktop**.
> You do **not** need Java or Maven installed locally if you run via Docker.

---

## 1. Architecture Overview

### Services

1. **faculty-service** (port `8083`)
   - Authentication & JWT (`/auth/register`, `/auth/login`)
   - User management (students, professors, admins)
   - Faculties and policies CRUD
   - Faculty rooms CRUD & "lock" / "unlock" / delete
   - When rooms are deleted/locked:
     - Calls **scheduling-service** to remove/cancel schedules
     - Calls **reservation-service** to revoke related reservations

2. **reservation-service** (port `8081`)
   - Reservation lifecycle:
     - Create reservation
     - Approve reservation (admin)
     - Revoke/cancel reservation
   - Talks to **scheduling-service** to:
     - Check availability
     - Create/remove room time slots
   - Endpoint to revoke all reservations by room (used when a room is deleted/locked).

3. **scheduling-service** (port `8082`)
   - Manages:
     - Rooms (id, name, capacity, equipment, locked flag)
     - Room schedules (blocked time slots tied to reservations)
   - Endpoints to:
     - CRUD rooms
     - Check availability for a room
     - Add/remove schedules for a room & reservation

All three services use **JWT** for security. You obtain a token from **faculty-service**
and send it as:

```http
Authorization: Bearer <token>
```

---

## 2. Prerequisites (Windows)

1. **Docker Desktop**
   - Install from the official Docker website.
   - During install, make sure **"Use WSL 2 based engine"** is enabled.
   - After installation, start Docker Desktop and wait until it says
     **"Docker is running"**.

2. **Git** (optional but recommended)
   - To clone the repo. Otherwise you can just unzip the project somewhere.

> If you’re only running via Docker, you **do not need** to install Java or Maven locally.

---

## 3. Project Layout

After unzipping/cloning, you should have something like:

```text
microservices/
  docker-compose.yml

  reservation-service/
    pom.xml
    src/...
    Dockerfile

  scheduling-service/
    pom.xml
    src/...
    Dockerfile

  faculty-service/
    pom.xml
    src/...
    Dockerfile
```

`docker-compose.yml` lives inside the `microservices` folder and knows how to start:

- 3 Postgres databases (one per service)
- 3 Spring Boot services

---

## 4. Running Everything with Docker (Recommended)

### Step 1 – Open a terminal in the `microservices` folder

On Windows you can use **PowerShell**, **CMD**, or **Git Bash**.

```powershell
cd path	o\your\project\microservices
# example:
# cd C:\Users\you\Documents\campus-reservations\microservices
```

### Step 2 – Build & start the stack

With Docker Compose v2 (what Docker Desktop uses by default now):

```powershell
docker compose up --build
```

If you have an older Docker setup that still uses the older CLI name:

```powershell
docker-compose up --build
```

What this does:

- Builds Docker images for:
  - `reservation-service`
  - `scheduling-service`
  - `faculty-service`
- Starts 3 Postgres containers (one per service)
- Starts the three Spring Boot apps

On first run you’ll see Maven downloading dependencies – this can take a bit.

### Step 3 – Wait for services to start

Watch the logs; when you see messages like:

- `Started ReservationServiceApplication`
- `Started SchedulingServiceApplication`
- `Started FacultyServiceApplication`

…you’re ready.

The services will be reachable at:

- Faculty: `http://localhost:8083`
- Reservation: `http://localhost:8081`
- Scheduling: `http://localhost:8082`

### Step 4 – Default admin user

On startup, **faculty-service** creates a default admin user (if not present), e.g.:

- **username**: `admin`
- **password**: `admin`

If you later change these in the code, adjust here accordingly.

---

## 5. Basic Usage Flow (via Postman or similar)

Below is a **minimal flow** you can use to test the system and also to build your
Postman collection.

### 5.1 Authenticate & get JWT

**POST** `http://localhost:8083/auth/login`

```json
{
  "username": "admin",
  "password": "admin"
}
```

Response:

```json
{
  "token": "<JWT_TOKEN_HERE>"
}
```

Use this token for all protected endpoints:

```text
Authorization: Bearer <JWT_TOKEN_HERE>
```

---

### 5.2 Create a faculty (admin)

**POST** `http://localhost:8083/faculties`

Headers:

- `Authorization: Bearer <token>`

Body:

```json
{
  "facultyId": "ENG",
  "name": "Engineering"
}
```

---

### 5.3 Create a room via faculty-service (admin)

**POST** `http://localhost:8083/faculty-rooms`

Headers:

- `Authorization: Bearer <token>`

Example body:

```json
{
  "roomId": "ENG-101",
  "facultyId": "ENG",
  "name": "ENG Lecture Hall 101",
  "capacity": 100,
  "equipment": "projector,whiteboard"
}
```

This:

- Persists the room in `faculty-service`
- Calls **scheduling-service** to create a corresponding room record for scheduling

---

### 5.4 Create a reservation as a normal user

1. Register a student (optional if you want non-admin users):

   **POST** `http://localhost:8083/auth/register`

   ```json
   {
     "username": "alice",
     "password": "password",
     "facultyId": "ENG",
     "role": "STUDENT"
   }
   ```

2. Login as that user:

   **POST** `http://localhost:8083/auth/login`

   ```json
   {
     "username": "alice",
     "password": "password"
   }
   ```

   Save this **student token**.

3. Use that token to create a reservation:

   **POST** `http://localhost:8081/reservations`

   Headers:

   - `Authorization: Bearer <student_token>`

   Example body:

   ```json
   {
     "roomId": "ENG-101",
     "facultyId": "ENG",
     "startTime": "2025-05-10T10:00:00",
     "endTime": "2025-05-10T12:00:00",
     "purpose": "Study group",
     "requiredEquipment": ["projector"]
   }
   ```

   The reservation-service will:

   - Check availability via **scheduling-service**
   - If available, create a schedule entry
   - Persist reservation with status `PENDING` (or `APPROVED` depending on logic)

---

### 5.5 Approve or revoke a reservation (admin)

Using **admin token**:

- **Approve**:

  **POST** `http://localhost:8081/reservations/{id}/approve`

- **Revoke**:

  **POST** `http://localhost:8081/reservations/{id}/revoke`

- **Revoke all reservations for a room** (used when room deleted/locked):

  **DELETE** `http://localhost:8081/reservations/by-room/ENG-101`

---

### 5.6 Lock / delete a room (admin)

Using admin token:

- **Lock room** (prevents future reservations and cancels schedules):

  **POST** `http://localhost:8083/faculty-rooms/ENG-101/lock`

  This will:
  - Mark the room as locked in faculty DB
  - Call scheduling-service to cancel schedules
  - Call reservation-service to revoke related reservations

- **Delete room**:

  **DELETE** `http://localhost:8083/faculty-rooms/ENG-101`

  This will:
  - Remove the room from faculty DB
  - Ask scheduling-service to remove associated room/schedules
  - Ask reservation-service to revoke related reservations

---

## 6. Stopping the Stack

From the same `microservices` folder:

```powershell
docker compose down
```

This stops and removes containers, but keeps volumes (your data).

If you also want to remove data volumes:

```powershell
docker compose down -v
```

---

## 7. Running Services Without Docker (Optional)

If you prefer to run services directly (for debugging):

1. Install **JDK 21** and **Maven** on your machine.
2. Start Postgres yourself or reuse the DB containers from Docker Compose.
3. For each service:

   ```powershell
   cd reservation-service
   mvn spring-boot:run
   ```

   Do the same for `scheduling-service` and `faculty-service`.

You’ll need to ensure the `spring.datasource.url` points to the right database
host (e.g. `localhost` instead of `reservation-db` when not using Docker).

---

## 8. Troubleshooting

- **"docker: command not found" or "docker compose: not found"**
  - Ensure Docker Desktop is installed and running.
  - Open a new PowerShell / CMD window after installation.

- **Ports already in use (8081/8082/8083)**
  - Stop any other apps using those ports.
  - Or edit the `server.port` in each service’s `application.yml` and rebuild.

- **Database connection errors on first startup**
  - Sometimes services start faster than Postgres.
  - Just run `docker compose down` and `docker compose up` again.
