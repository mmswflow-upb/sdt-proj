# Postman Collections for Campus Reservation API (Rewritten)

This folder contains **fresh Postman collections** that mirror the main K6 workflows,
but are rebuilt from scratch to use **only environment variables** (no collection variables).

## Files

- `Campus-Reservation-Local.postman_environment.json`  
  Shared environment with:
  - `base_url` (e.g. `http://localhost:8080`)
  - credentials for admin, student, and faculty admin
  - runtime values such as `admin_token`, `student_token`, `faculty_admin_token`,
    `faculty_id`, `room_id`, `reservation_id`, etc.

- `01-Setup.postman_collection.json`  
  Run this **first**. It:
  1. Logs in as admin
  2. Registers + logs in a student
  3. Registers + logs in a faculty admin
  4. Creates a test faculty
  5. Creates a test room under that faculty  
  All IDs and tokens are stored in **environment variables**.

- `02-Student-Workflow.postman_collection.json`  
  Core student journey:
  1. Prepare a reservation time window (tomorrow, +2h)
  2. Check room availability
  3. Create a reservation
  4. Get student's own reservations
  5. Cancel the created reservation

- `03-Admin-Workflow.postman_collection.json`  
  Basic admin operations:
  1. List all reservations
  2. Approve a reservation
  3. Revoke the same reservation
  4. Approve it again
  5. List all rooms

- `04-Edge-Cases.postman_collection.json`  
  Selected edge cases:
  1. Create a base reservation for the student
  2. Attempt a **duplicate** reservation for the same slot (should fail)
  3. Register & login a second student
  4. Have the second student attempt a **conflicting** reservation (should fail)

- `Authorization-Tests.postman_collection.json`  
  Negative authorization tests:
  1. Student tries to approve a reservation (should be blocked)
  2. Student tries to create a room (should be blocked)
  3. Student tries to create a faculty (should be blocked)
  4. Faculty admin tries to create a faculty (should be blocked)

## Usage

1. Import the **environment** file into Postman:
   - `Campus-Reservation-Local.postman_environment.json`

2. Import the collections you need (or all of them).

3. Select the *Campus Reservation Local* environment in the top-right corner of Postman.

4. Run the collections in this order:

   1. `01-Setup`
   2. `02-Student-Workflow` (creates a reservation and sets `reservation_id`)
   3. `03-Admin-Workflow` (operates on `reservation_id`)
   4. `04-Edge-Cases`
   5. `Authorization-Tests`

5. All dynamic data (tokens, IDs, times) flows through **environment variables** only.

You can tweak usernames/passwords, base URL, and other inputs directly in the environment
without touching the collections.
