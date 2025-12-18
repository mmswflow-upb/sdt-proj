# CI/CD Pipeline

This project uses GitHub Actions with a self-hosted runner for automated deployment.

## Workflow Steps

When code is pushed to 5-microservices-extended:

1. Verifies Docker is running
2. Removes old containers and images
3. Creates .env file from .env.example
4. Builds all microservices (runs unit tests during build)
5. Starts all containers
6. Shows running containers and logs

## Unit Testing

Unit tests run during the Docker build phase using Maven:
- JUnit 5 and Mockito for service layer testing
- Tests cover reservations, user management, scheduling, and faculty operations
- If any test fails, the Docker build stops and deployment is prevented
- Tested services: ReservationService, UserService, FacultyService, SchedulingService

See [UNIT-TESTS.md](UNIT-TESTS.md) for details.

## Requirements

Docker Desktop must be running before triggering the workflow. The workflow verifies Docker availability at the start.

## Security

The self-hosted runner only picks up jobs from this repository. Forked repositories cannot access our runner, so they cannot trigger deployments on our machine even if they push commits with the workflow file.

Only users with write access to this repository can push commits that trigger the workflow.

## Configuration

Workflow file: `.github/workflows/deploy-local.yml`

## Environment Variables

The workflow creates a .env file from .env.example before building. This provides database credentials, JWT secret, and RabbitMQ settings to all microservices.
