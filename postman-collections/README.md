# Postman Collections

This folder contains Postman collections organized by workflow, matching the K6 test structure. Each collection is a separate file that can be imported and run independently.

## Collections Overview

### 01-Setup.postman_collection.json
**Run this FIRST** - Sets up authentication and basic resources:
- Register Admin, Student, and Faculty Admin users
- Create initial faculty
- Create initial room
- Saves all tokens and IDs for other collections

### 02-Student-Workflow.postman_collection.json
Complete student user journey:
- Check room availability
- Create reservations
- View own reservations
- Cancel reservations

### 03-Admin-Workflow.postman_collection.json
Admin operations:
- View all reservations
- Approve reservations
- Revoke reservations
- Create faculties and rooms
- Delete rooms (cascade deletion test)

### 04-Edge-Cases.postman_collection.json
Error handling and validation:
- Double booking attempts (409 Conflict)
- Invalid time ranges (400 Bad Request)
- Past time reservations (400 Bad Request)
- Non-existent resources (404 Not Found)
- Unauthorized access (401 Unauthorized)
- Locked room reservations (403 Forbidden)

### Authorization-Tests.postman_collection.json
Role-based access control verification:
- Students cannot approve/revoke reservations
- Students cannot create/delete rooms
- Students cannot create/delete faculties
- Faculty admins cannot create/delete faculties

### Campus-Reservation-Main.postman_collection.json
All-in-one collection combining all workflows (use this if you want everything in one file)

## Quick Start

### Option 1: Run Individual Collections (Recommended)

1. Import all `.json` files into Postman
2. Run collections in order:
   - **01-Setup** (run once to initialize)
   - **02-Student-Workflow** (test student features)
   - **03-Admin-Workflow** (test admin features)
   - **04-Edge-Cases** (test error handling)
   - **Authorization-Tests** (test security)

### Option 2: Run All-in-One

1. Import `Campus-Reservation-Main.postman_collection.json`
2. Click "Run" to execute all tests sequentially

## How Variables Work

Each collection has variables that store values across requests:

- `base_url` - API gateway URL (default: http://localhost:8080/api)
- `admin_token` - Admin JWT (set by Setup)
- `student_token` - Student JWT (set by Setup)
- `faculty_admin_token` - Faculty admin JWT (set by Setup)
- `faculty_id` - Created faculty ID (set by Setup)
- `room_id` - Created room ID (set by Setup)
- `reservation_id` - Latest reservation ID (set by create reservation)

**Variables are automatically saved** when you run requests - no manual copying needed!

## Running Collections

### Individual Request
Click any request → **Send**

### Entire Collection
Right-click collection → **Run collection** → **Run**

### With Collection Runner
1. Click **Runner** (bottom right)
2. Drag collections in order
3. Set delay between requests (500ms recommended)
4. Click **Run**

## Expected Results

### Success Tests (200/201/204)
- Setup collection: All requests should succeed
- Student workflow: Check availability, create, view, cancel
- Admin workflow: View, approve, revoke, manage resources

### Failure Tests (400/401/403/404/409)
- Edge cases: All should fail with expected error codes
- Authorization tests: All should be blocked (403/401)

**Green checkmarks** = test passed (includes expected failures!)
**Red X** = unexpected result

## Matching K6 Workflows

| Postman Collection | K6 Workflow File |
|-------------------|------------------|
| 01-Setup | auth-setup.js + common-setup.js |
| 02-Student-Workflow | student-workflow.js + student-reservations.js |
| 03-Admin-Workflow | admin-workflow.js + room-deletion.js |
| 04-Edge-Cases | edge-cases-workflow.js |
| Authorization-Tests | authorization-tests.js |

The K6 tests are more comprehensive and include load testing, but these Postman collections cover all the main functionality and edge cases.

## Tips

- **First time**: Run 01-Setup to populate variables
- **Testing changes**: Re-run specific collection
- **Debugging**: Run individual requests
- **Variables not set?**: Check 01-Setup ran successfully
- **Token expired?**: Re-run authentication requests in 01-Setup

## Advanced: Newman CLI

Run collections from command line:

```bash
npm install -g newman
newman run 01-Setup.postman_collection.json
newman run 02-Student-Workflow.postman_collection.json
```

Perfect for CI/CD pipelines!
