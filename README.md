# Campus Room Reservation System

A microservices-based room reservation system built for milestone 5.

## The Implementation

The system is made up of five main parts:

- **Gateway Service** - The front door for everything. Routes requests to the right service and handles authentication. Runs on port 8080.

- **Faculty Service** - Takes care of user accounts (students, professors, admins), faculties, rooms, and policies. Also handles login/registration and generates JWT tokens. When rooms are deleted, it communicates with the other services to cascade those changes. Runs on port 8083.

- **Reservation Service** - Manages room reservations. Students create them, admins approve or revoke them. Before creating a reservation, it talks to the scheduling service to verify room availability. Publishes reservation events to RabbitMQ for notifications. Runs on port 8081.

- **Scheduling Service** - Keeps track of room schedules and availability. Makes sure nobody double-books a room. Other services call it to check availability and manage schedules. Runs on port 8082.

- **Notification Service** - Listens to reservation events (created, cancelled, revoked) from RabbitMQ and logs notifications. In a production environment, this would send emails, push notifications, or trigger webhooks. Runs on port 8084.

The faculty, reservation, and scheduling services each have their own PostgreSQL database for data isolation. The gateway and notification services are stateless and don't require databases.

## Message Queue Benefits

We use RabbitMQ for asynchronous communication between the Reservation Service and Notification Service. This architectural choice provides several key advantages:

**Improved Scalability:**
- Services scale independently without affecting each other
- Multiple notification service instances can process messages in parallel
- Message queues buffer traffic spikes, preventing service overload
- No blocking operations means reservation service handles more concurrent requests

**Fault Tolerance:**
- Messages persist in queues even if notification service crashes
- Automatic retry mechanisms for failed message processing
- No data loss during service failures or restarts
- Services remain loosely coupled, reducing cascading failures

**Performance:**
- Reservation operations complete immediately without waiting for notifications
- Non-blocking message publishing improves response times
- Background processing of notifications doesn't impact user-facing operations

**Disadvantages:**
- Added complexity with message broker infrastructure
- Eventual consistency instead of immediate consistency
- Requires monitoring of queue depths and message processing rates
- Debugging distributed workflows is more complex than synchronous calls

The notification service uses asynchronous messaging via RabbitMQ, which means reservation operations don't wait for notifications to be sent. For more details on our messaging architecture, see [MESSAGE-QUEUE.md](MESSAGE-QUEUE.md).

## Inter-Service Communication

The services talk to each other in a few key scenarios:

**Synchronous REST Communication:**
- **Reservation -> Scheduling**: When creating a reservation, checks room availability
- **Faculty -> Scheduling**: When deleting a room, removes associated schedules
- **Faculty -> Reservation**: When deleting a room, revokes associated reservations

**Asynchronous Messaging (RabbitMQ):**
- **Reservation -> Notification**: Publishes events when reservations are created, cancelled, or revoked
- The notification service listens to these events and processes them independently

All REST communication happens via HTTP, and each service validates JWT tokens independently for security. Feign clients are used to simplify inter-service REST calls.

Also, the JWTs of users are further passed with inter-service requests, so requests can be traced back to users.

## Setup and Running

### Prerequisites

- **Docker Desktop** (Windows/Mac) or **Docker + Docker Compose** (Linux)
- **Postman** (for testing the API)

### Step 1: Clone repo

Clone the repo and navigate to the project directory, add a `.env` file using the [example](.env.example)

### Step 2: Run Services

On Windows/Mac, ensure Docker Desktop is running, then execute in the terminal:

```bash
docker compose up -d --build
```

This command will:

- Build all five microservices (gateway, faculty, reservation, scheduling, notification)
- Spin up three PostgreSQL databases
- Start RabbitMQ message broker
- Start everything on their respective ports

Give it about a minute to fully start up. The system is ready when logs from all services appear.

### Step 3: Verify Services Are Running

Verify if images were created, there should be 5 images starting with `sdt-campus-*`, a `postgres:15` image, and `rabbitmq:3.12-management`:

```bash
docker images
```

Verify if containers are running:

```bash
docker ps
```

### Step 4: Monitor Notification Service Logs

The notification service logs all reservation events (created, cancelled, revoked). To view these logs in real-time:

```bash
docker logs -f sdt-campus-reservations-notification-service-1
```

To view logs from a specific service:

```bash
docker logs -f sdt-campus-reservations-reservation-service-1
docker logs -f sdt-campus-reservations-gateway-service-1
```

To stop following logs, press `Ctrl+C`.

### Step 5: Access RabbitMQ Management Console (Optional)

RabbitMQ provides a web-based management interface to monitor queues, exchanges, and messages:

- **URL**: <http://localhost:15672>
- **Username**: `guest` (or the `RABBITMQ_USERNAME` from `.env`)
- **Password**: `guest` (or the `RABBITMQ_PASSWORD` from `.env`)

Here we can see message flow, queue depths, and troubleshoot messaging issues.

### Step 6: Import Postman Collections

Navigate to the `postman-collections` folder in this repository. It contains:

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

Hover over the names of each collection, click on the 3 dots, select `Run`, then click on the `Run-CollectionName` button, please run them in this order:

1. **Setup** - Creates the foundation (admin login, faculty, rooms, policy, test users)
2. **Student-Workflow** - Tests student reservation flows (create, view, concurrent bookings)
3. **Admin-Workflow** - Tests admin operations (approve, revoke, room management)
4. **Edge-Cases** - Tests conflict resolution (duplicate bookings, overlapping times, cancellations)
5. **Authorization-Tests** - Verifies role-based access control

For detailed information about each collection's structure and workflow, [see here](postman-collections/README.md).

## CI/CD Pipeline

The project uses GitHub Actions with a self-hosted runner for automated deployment. When code is pushed to 5-microservices-extended:
- Verifies Docker is running
- Removes old containers and images
- Builds all microservices with unit tests
- Deploys containers locally

If unit tests fail during build, deployment stops.

**Requirements**: Docker Desktop must be running before the workflow starts.

**Security**: The self-hosted runner only executes workflows from this repository. Forks cannot trigger deployments on our machine because they lack access to the runner.

For more details, see [CI-CD-SETUP.md](CI-CD-SETUP.md).

## Testing

### Unit Tests
JUnit 5 + Mockito tests for core service layer components:
- Reservation management
- User registration and authentication
- Faculty and room operations
- Scheduling and availability logic

Tests run during Docker build. See [UNIT-TESTS.md](UNIT-TESTS.md).

### Integration Tests
Postman collections for end-to-end API testing across microservices. See [postman-collections/README.md](postman-collections/README.md).

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

**Services won't start**: Make sure ports 8080-8084, 5433-5435, 5672, and 15672 aren't being used by other applications.

**Connection errors between services**: Wait a full minute after running `docker compose up`. The databases and RabbitMQ need time to initialize before the services can connect.

**Authentication fails**: Ensure the token returned from login/register is used in the Authorization header as `Bearer <token>`.

**Notifications not appearing**: Check the notification-service logs with `docker logs -f sdt-campus-reservations-notification-service-1` and verify RabbitMQ is running with `docker ps | grep rabbitmq`.

**RabbitMQ connection errors**: Ensure RabbitMQ container is healthy. Check with `docker ps` and look at RabbitMQ logs with `docker logs sdt-campus-reservations-rabbitmq-1`.

## Tech Stack

- Java 21
- Spring Boot 3.1.5
- Spring Cloud OpenFeign
- Spring AMQP (RabbitMQ)
- PostgreSQL 15
- RabbitMQ 3.12
- Docker & Docker Compose
- JWT for authentication
- Postman for testing
