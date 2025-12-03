# K6 Workflow Testing Suite

This directory contains k6 workflow testing scripts for the Campus Reservations microservices system. The tests are organized by user role and include comprehensive edge case coverage.

## Prerequisites

1. **Install k6**: https://k6.io/docs/getting-started/installation/
   
   ```powershell
   # Windows (using Chocolatey)
   choco install k6
   
   # Or download from: https://github.com/grafana/k6/releases
   ```

2. **Ensure services are running**:
   ```powershell
   docker compose up -d
   ```

3. **Verify gateway is accessible**:
   ```powershell
   curl http://localhost:8080/actuator/health
   ```

## Quick Start

**Run all workflows:**
```powershell
k6 run k6-tests/run-all-workflows.js
```

**Run specific workflow:**
```powershell
# Admin workflow
k6 run k6-tests/workflows/admin-workflow.js

# Faculty admin workflow
k6 run k6-tests/workflows/faculty-admin-workflow.js

# Student workflow
k6 run k6-tests/workflows/student-workflow.js

# Edge cases
k6 run k6-tests/workflows/edge-cases-workflow.js
```

---

## Workflow Tests

The test suite is organized into modular workflows that test different user roles and edge cases:

### 1. Admin Workflow (`workflows/admin-workflow.js`)
**Tests full admin capabilities:**
- Create faculties
- List all faculties
- Create rooms with full permissions
- View all system reservations
- Update faculty policies

**Run:**
```powershell
k6 run k6-tests/workflows/admin-workflow.js
```

---

### 2. Faculty Admin Workflow (`workflows/faculty-admin-workflow.js`)
**Tests faculty-level admin capabilities:**
- Register and login as faculty admin
- List faculty-scoped rooms
- View reservations
- **Edge case**: Attempt to create faculty (should fail - permission denied)

**Run:**
```powershell
k6 run k6-tests/workflows/faculty-admin-workflow.js
```

---

### 3. Student Workflow (`workflows/student-workflow.js`)
**Tests complete student reservation flow:**
- Register and login as student
- Check room availability
- Create reservation
- View own reservations
- **Edge case**: Attempt to approve own reservation (should fail)
- Admin approves the reservation
- Verify approval status

**Run:**
```powershell
k6 run k6-tests/workflows/student-workflow.js
```

---

### 4. Edge Cases Workflow (`workflows/edge-cases-workflow.js`)
**Tests conflict detection and validation:**
1. **Duplicate reservation**: Same user tries to create identical reservation → should be rejected
2. **Conflicting reservation**: Different user tries to book same room/time → should be rejected (409)
3. **Overlapping time slot**: User tries to book partial overlap → should be rejected
4. **Back-to-back reservations**: User books immediately after another ends → should succeed
5. **Unauthorized revoke**: Student tries to revoke another student's reservation → should fail (403)

**Run:**
```powershell
k6 run k6-tests/workflows/edge-cases-workflow.js
```

---

## Custom Metrics

The workflow tests track specific metrics:

- **admin_workflows** - Admin workflow completions
- **faculty_admin_workflows** - Faculty admin workflow completions
- **student_workflows** - Student workflow completions
- **conflict_detected** - Correctly rejected conflicts
- **duplicate_rejected** - Correctly rejected duplicates
- **unauthorized_blocked** - Correctly blocked unauthorized actions
- **workflow_duration** - Time to complete workflows

---

## Environment Variables

All workflow scripts support the following environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | Gateway URL |
| `ADMIN_USERNAME` | `admin` | Admin username |
| `ADMIN_PASSWORD` | `admin` | Admin password |

**Example:**
```powershell
k6 run k6-tests/run-all-workflows.js `
  --env BASE_URL=http://localhost:8080 `
  --env ADMIN_USERNAME=admin `
  --env ADMIN_PASSWORD=admin
```

---

## Test Output

### Console Output
Default output shows real-time metrics and detailed workflow progress in the terminal.

### JSON Output
```powershell
k6 run k6-tests/run-all-workflows.js --out json=results.json
```

### CSV Output
```powershell
k6 run k6-tests/run-all-workflows.js --out csv=results.csv
```

---

## Expected Results

### Successful Test Run

✅ **All workflows should:**
- Complete without errors
- Validate all permission boundaries
- Detect and reject conflicts (409 status)
- Detect and reject duplicates
- Block unauthorized actions (403/401 status)

### Thresholds

- HTTP error rate < 10%
- All role workflows complete successfully
- Conflict detection count > 0
- Duplicate rejection count > 0
- Unauthorized blocking count > 0

---

## Troubleshooting

### "Connection refused" errors
- Verify services are running: `docker compose ps`
- Check gateway is accessible: `curl http://localhost:8080/actuator/health`

### High error rates
- Check service logs: `docker compose logs gateway-service`
- Verify database connections
- Check resource limits (CPU, memory)

### Workflow failures
- Review console output for specific error messages
- Check that test data is properly created in setup phase
- Verify JWT tokens are valid

### Permission errors (403/401)
These are **expected** for edge case testing:
- Students cannot approve reservations
- Faculty admins cannot create faculties
- Students cannot revoke others' reservations

---

## Example Test Run

```powershell
# 1. Start services
docker compose up -d

# 2. Wait for services to be healthy
Start-Sleep -Seconds 30

# 3. Run all workflows
k6 run k6-tests/run-all-workflows.js

# 4. Run specific workflow if needed
k6 run k6-tests/workflows/student-workflow.js

# 5. Save results to JSON
k6 run k6-tests/run-all-workflows.js --out json=workflow-results.json
```

---

## Project Structure

```
k6-tests/
├── workflows/
│   ├── common-setup.js           # Shared setup/teardown
│   ├── admin-workflow.js         # Admin role tests
│   ├── faculty-admin-workflow.js # Faculty admin tests
│   ├── student-workflow.js       # Student tests
│   ├── edge-cases-workflow.js    # Edge cases & conflicts
│   └── README.md                 # Detailed workflow docs
├── run-all-workflows.js          # Main test runner
└── README.md                     # This file
```

For detailed workflow documentation, see [workflows/README.md](workflows/README.md).

---

## Contributing

When adding new workflow scenarios:
1. Create a new file in `workflows/` directory
2. Follow the existing workflow structure
3. Export the workflow function
4. Add it to `run-all-workflows.js`
5. Update both README files
6. Include appropriate checks and metrics
5. Test your script before committing
