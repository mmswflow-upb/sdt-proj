# K6 Workflow Tests

This directory contains modular k6 test scripts organized by workflow type.

## Structure

```
workflows/
├── common-setup.js          # Shared setup and teardown functions
├── admin-workflow.js        # Admin role workflow tests
├── faculty-admin-workflow.js # Faculty admin role workflow tests
├── student-workflow.js      # Student role workflow tests
└── edge-cases-workflow.js   # Edge case and conflict testing
```

## Running Tests

### Run All Workflows Together
```powershell
k6 run k6-tests/run-all-workflows.js
```

### Run Individual Workflows

**Admin Workflow Only:**
```powershell
k6 run k6-tests/workflows/admin-workflow.js
```

**Faculty Admin Workflow Only:**
```powershell
k6 run k6-tests/workflows/faculty-admin-workflow.js
```

**Student Workflow Only:**
```powershell
k6 run k6-tests/workflows/student-workflow.js
```

**Edge Cases Only:**
```powershell
k6 run k6-tests/workflows/edge-cases-workflow.js
```

## Workflow Details

### 1. Admin Workflow (`admin-workflow.js`)
Tests full admin capabilities:
- Create faculties
- List all faculties
- Create rooms
- List all reservations
- Update policies

### 2. Faculty Admin Workflow (`faculty-admin-workflow.js`)
Tests faculty-level admin capabilities:
- Register and login as faculty admin
- List faculty-scoped rooms
- View reservations
- **Edge case**: Attempt to create faculty (should fail)

### 3. Student Workflow (`student-workflow.js`)
Tests complete student reservation flow:
- Register and login
- Check room availability
- Create reservation
- View own reservations
- **Edge case**: Attempt to approve own reservation (should fail)
- Admin approves reservation
- Verify approval

### 4. Edge Cases Workflow (`edge-cases-workflow.js`)
Tests conflict detection and validation:
- **Duplicate reservation**: Same user, same time/room
- **Conflicting reservation**: Different user, same time/room
- **Overlapping time slot**: Partial overlap with existing reservation
- **Back-to-back reservation**: Should succeed
- **Unauthorized revoke**: Student tries to revoke another's reservation

## Custom Metrics

Each workflow tracks specific metrics:

- `admin_workflows` - Admin workflow completions
- `faculty_admin_workflows` - Faculty admin workflow completions
- `student_workflows` - Student workflow completions
- `conflict_detected` - Correctly rejected conflicts
- `duplicate_rejected` - Correctly rejected duplicates
- `unauthorized_blocked` - Correctly blocked unauthorized actions
- `workflow_duration` - Time to complete workflows

## Environment Variables

All workflows support:

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

## Common Setup

The `common-setup.js` module provides shared setup/teardown:
- Admin login
- Test faculty creation
- Test room creation (3 rooms)
- Faculty policy creation

All workflows use this common setup for consistency.

## Thresholds

Each workflow has appropriate thresholds:
- HTTP error rate < 10%
- Workflow completion count > 0
- Conflict/duplicate detection (for edge cases)

## Integration

Import workflows in your own scripts:

```javascript
import { adminWorkflow } from './workflows/admin-workflow.js';
import { setupTestEnvironment } from './workflows/common-setup.js';

export function setup() {
  return setupTestEnvironment();
}

export default function(data) {
  adminWorkflow(data);
}
```
