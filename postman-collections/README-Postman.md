# Postman Collections for Campus Reservation API

## Files

- `Campus-Reservation-Local.postman_environment.json`  
  Shared environment with:
  - Base configuration: `base_url`, `admin_username`, `student_username`, `faculty_admin_username`, `student2_username`, etc.
  - Tokens: `admin_token`, `student_token`, `faculty_admin_token`, `student2_token`
  - Core IDs: `faculty_id`, `faculty_external_id`, `policy_id`, `room_id_1`, `room_id_2`, `room_id_3`
  - Workflow data: `main_reservation_id`, concurrent reservation IDs, edge / cancel / deletion reservation IDs.

  - All dynamic data flows via **environment variables**.

  We used pre-request and post-request scripts (`JS` + `Postman Library`) to change these env vars so that requests adapted their headers and bodies according to the responses coming from the API, but also to add checks for HTTP status codes and to generate timestamps that were used in naming rooms, faculties, etc (for uniqueness).

- `1-Setup.postman_collection.json`:
  1. Login admin
  2. Create test faculty
  3. Steps 3–5: Create three rooms under that faculty
  4. Step 6: Create a faculty policy
  5. Steps 7–8: Register & login Student1
  6. Steps 9–10: Register & login Faculty Admin
  7. Steps 11–12: Register & login Student2

- `2-Student-Workflow.postman_collection.json`:
  - Computes a reservation window (48h ahead, 2h duration)
  - Checks availability (`GET /api/availability?roomId&from&to`)
  - Creates a main reservation (`POST /api/reservations` with `startDateTime` / `endDateTime` / `attendees`)
  - Lists all own reservations (`GET /api/reservations/me`)
  - Lists pending reservations (`GET /api/reservations/me?status=PENDING`)
  - Creates three concurrent reservations (one per room)
  - Gets a reservation by ID (`GET /api/reservations/{id}`) as student

- `3-Admin-Workflow.postman_collection.json`:
  - Admin lists reservations
  - Admin approves, revokes, and re-approves the main reservation
  - Student views revoked reservations (`status=REVOKED`)
  - Faculty admin and admin approve concurrent reservations
  - Admin updates the faculty policy (`PUT /api/policies/{id}`)
  - Admin creates an extra faculty
  - Admin creates and lists rooms
  - Faculty admin creates, lists, and **deletes** a room in their faculty

- `4-Edge-Cases.postman_collection.json`: 
  - Duplicate booking by same student (should fail)
  - Conflicting booking by another student (should fail)
  - Overlapping reservation (should fail)
  - Back-to-back reservation (should succeed)
  - Unauthorized revoke by another student
  - Cancellation flow: create → approve → cancel → immutable re-approve → rebook → other student cannot cancel
  - Room deletion cascade: create room, create & approve reservation, delete room, check room 404,
    check schedule, and verify availability behaves correctly

- `5-Authorization-Tests.postman_collection.json`:
  - Student cannot:
    - Approve reservations
    - Revoke reservations
    - Create rooms
    - Delete rooms
    - Create faculties
    - Delete faculties
  - Faculty admin cannot:
    - Create faculties
    - Delete faculties

## Recommended Run Order

1. Import the **environment** file and all 5 collections into Postman.
2. Select the *Campus Reservation Local* environment.
3. Run the collections in this order:

   1. `1-Setup`
   2. `2-Student-Workflow`
   3. `3-Admin-Workflow`
   4. `4-Edge-Cases`
   5. `5-Authorization-Tests`

## Every endpoint used

- `/api/auth/register`, `/api/auth/login`
- `/api/faculties`, `/api/faculties/{id}`
- `/api/rooms`, `/api/rooms/{id}`
- `/api/policies`, `/api/policies/{id}`
- `/api/availability?roomId&from&to`
- `/api/reservations` (create)
- `/api/reservations/me`, `/api/reservations/me?status=...`
- `/api/reservations/{id}`, `/api/reservations/{id}/approve`, `/api/reservations/{id}/revoke`, `/api/reservations/{id}/cancel`
- `/api/reservations` (admin list)
- `/api/schedules/{id}`
