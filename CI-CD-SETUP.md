# CI/CD Setup with GitHub Actions Self-Hosted Runner

This project uses GitHub Actions with a self-hosted runner for automatic local deployment.

## What This Does

When you push code to `main` or `5-microservices-extended` branches, GitHub Actions will:
1. Pull the latest code on your PC
2. Rebuild all Docker images
3. Restart all services with docker-compose
4. Show deployment status

## Setup Instructions

### Step 1: Install Prerequisites
- Docker Desktop (already installed)
- Git (already installed)
- Internet connection

### Step 2: Set Up Self-Hosted Runner

1. **Go to your GitHub repository**: https://github.com/mmswflow-upb/sdt-proj

2. **Navigate to Settings**:
   - Click on `Settings` tab
   - In the left sidebar, click `Actions` → `Runners`

3. **Add a new runner**:
   - Click the green `New self-hosted runner` button
   - Select `Windows` as the operating system
   - Select `x64` as the architecture

4. **Follow GitHub's download instructions** (example):
   ```powershell
   # Create a folder for the runner
   mkdir actions-runner; cd actions-runner
   
   # Download the latest runner package
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner-win-x64-2.311.0.zip
   
   # Extract the installer
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/actions-runner-win-x64-2.311.0.zip", "$PWD")
   ```

5. **Configure the runner**:
   ```powershell
   # Run the configuration script (use the token from GitHub UI)
   ./config.cmd --url https://github.com/mmswflow-upb/sdt-proj --token YOUR_TOKEN_FROM_GITHUB
   
   # When prompted:
   # - Runner name: press Enter for default or choose a name (e.g., "mario-local")
   # - Runner group: press Enter for default
   # - Labels: press Enter for default
   # - Work folder: press Enter for default
   ```

6. **Run the runner**:
   
   **Option A - Run in current terminal** (stops when you close terminal):
   ```powershell
   ./run.cmd
   ```
   
   **Option B - Install as Windows Service** (runs in background, starts on boot):
   ```powershell
   # Run PowerShell as Administrator, then:
   cd path\to\actions-runner
   ./svc.cmd install
   ./svc.cmd start
   ```

### Step 3: Test the Deployment

1. Make a small change to any file (e.g., add a comment to README.md)
2. Commit and push:
   ```bash
   git add .
   git commit -m "Test CI/CD pipeline"
   git push
   ```
3. Go to your repo → `Actions` tab on GitHub
4. Watch your workflow run!

### Step 4: Monitor Deployments

- **GitHub UI**: Check the `Actions` tab to see workflow runs
- **Local logs**: The runner will show output in its terminal/service logs
- **Docker**: Run `docker-compose ps` to see running services
- **Service logs**: Run `docker-compose logs -f [service-name]`

## Manual Deployment Trigger

You can also trigger deployment manually without pushing code:
1. Go to repo → `Actions` tab
2. Click `Local Docker Deployment` workflow
3. Click `Run workflow` button
4. Select branch and click green `Run workflow`

## Troubleshooting

### Runner not appearing online
- Ensure the runner process is running (`./run.cmd` or check Windows Service)
- Check firewall isn't blocking GitHub connections
- Verify runner is configured for the correct repository

### Docker commands failing
- Make sure Docker Desktop is running
- Verify runner has permission to access Docker
- Check docker-compose.yml is valid: `docker-compose config`

### Build failures
- Check logs in GitHub Actions tab
- Ensure all environment variables are set
- Verify Docker has enough resources (Settings → Resources in Docker Desktop)

### Port conflicts
- Ensure no other services are using ports: 5433, 5434, 5435, 5672, 15672, 8081, 8082
- Stop conflicting services or update ports in docker-compose.yml

## Stopping the Runner

**If running in terminal**:
- Press `Ctrl+C`

**If running as service**:
```powershell
# Run as Administrator
cd path\to\actions-runner
./svc.cmd stop
./svc.cmd uninstall  # To completely remove the service
```

## Workflow Configuration

The workflow file is located at: `.github/workflows/deploy-local.yml`

You can customize:
- Which branches trigger deployment (currently: main, 5-microservices-extended)
- Build options (e.g., remove `--no-cache` for faster builds)
- Add testing steps before deployment
- Add notifications (email, Slack, etc.)

## Security Notes

- The runner has access to your Docker daemon and file system
- Only you can trigger workflows on your self-hosted runner
- Keep the runner token secure (it's displayed only once during setup)
- Don't share the runner with untrusted repositories

## Next Steps

Consider adding:
- Health checks for each service before completing deployment
- Automated tests that run before deployment
- Database migration steps
- Rollback mechanism if deployment fails
- Notifications on success/failure (Discord, email, etc.)
