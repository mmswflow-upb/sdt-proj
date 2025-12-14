# CI/CD Pipeline

This project uses GitHub Actions with a self-hosted runner for automatic deployment and testing.

## How It Works

When code is pushed to `main` or `5-microservices-extended` branches:

1. Checks if user is authorized
2. Starts Docker Desktop
3. Creates .env file from .env.example
4. Removes old containers and images
5. Builds all microservices
6. Starts all containers
7. Waits for gateway to be ready
8. Runs all Postman test collections
9. Uploads test reports

## Test Collections

The pipeline runs 5 Postman collections in order:

1. `1-Setup` - Creates users, faculty, rooms, and policies
2. `2-Student-Workflow` - Tests student reservation flow
3. `3-Admin-Workflow` - Tests admin approval and management
4. `4-Edge-Cases` - Tests duplicate bookings, conflicts, cascades
5. `5-Authorization-Tests` - Verifies role permissions

## Token Persistence

JWT tokens from Setup tests are exported to `environment.json` and reused in subsequent test collections. This allows authenticated requests to work across all test runs.

## Test Reports

After each workflow run, HTML test reports are uploaded as artifacts in GitHub Actions. Reports are kept for 30 days and can be downloaded from the workflow run page.

## Authorization

Only authorized users can trigger deployments. The workflow checks if the user is in the approved list or is a repository contributor before running.

Approved users can be added in `.github/workflows/deploy-local.yml` line 20:
```yaml
ALLOWED_USERS="mmswflow-upb username2 username3"
```

## Configuration

The workflow file is at `.github/workflows/deploy-local.yml`

Key configuration options:
- Trigger branches: lines 4-7
- Docker startup timeout: line 78, default 120 seconds
- Gateway health timeout: line 133, default 120 seconds
- Test report retention: 30 days

## Environment Variables

The pipeline creates a `.env` file from `.env.example` before building services. This ensures all microservices have the required configuration including database credentials, JWT secret, and RabbitMQ settings.
