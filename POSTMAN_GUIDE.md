# Postman Collection Guide

This guide explains how to use the Postman collections to test the Campus Room Reservation System. The collections are organized to match the K6 test workflows.

## Importing the Collection

1. Open Postman
2. Click **Import** (top left corner)
3. Select `postman-collections/Campus-Reservation-Main.postman_collection.json`
4. The collection will appear in your sidebar

## How Postman Collections Work

### Automatic Variable Management

Postman collections use **variables** to store values across requests. In our collection:

- **JWT tokens** are automatically saved when you login/register
- **IDs** (faculty_id, room_id, reservation_id) are captured from responses
- **Variables persist** across all requests in the collection
- **No manual copying** - everything flows automatically

### Running Sequentially

You can run requests in two ways:

1. **Individually**: Click a request → Send (useful for debugging)
2. **As a Group**: Right-click a folder → "Run folder" (runs all requests in order)
3. **Entire Collection**: Click collection → "Run" → executes everything top-to-bottom

### Test Assertions

Each request has a **Tests** tab that:

- Validates the response (status code, response structure)
- Extracts and saves variables for later requests
- Marks requests as pass/fail based on assertions

Green checkmarks = assertions passed
Red X = assertions failed (might be expected for edge cases!)

## Collection Structure

The collection is organized into folders:

- **Authentication** - Register and login endpoints
- **Faculties** - Faculty CRUD operations
- **Rooms** - Room management (create, lock, unlock, delete)
- **Reservations** - Full reservation lifecycle
- **Scheduling** - Room availability checks
- **Edge Cases** - Testing error scenarios

## Variables

The collection uses variables that auto-populate as you make requests:

- `base_url` - Gateway URL (http://localhost:8080)
- `admin_token` - JWT token for admin user
- `student_token` - JWT token for student user
- `faculty_id` - Created faculty ID
- `room_id` - Created room ID
- `reservation_id` - Created reservation ID

## Typical Workflow

### 1. Authentication Setup

Run these first to set up your users:

1. **Register Admin** - Creates admin account and saves token
2. **Register Student** - Creates student account and saves token

The tokens are automatically saved and used in subsequent requests.

### 2. Create Infrastructure

3. **Create Faculty** - Admin creates a faculty (saves faculty_id)
4. **Create Room** - Admin creates a room in that faculty (saves room_id)

### 3. Student Reservation Flow

5. **Create Reservation** - Student books a room (saves reservation_id)
6. **Get My Reservations** - Student views their bookings
7. **Approve Reservation** - Admin approves the booking
8. **Cancel Reservation** - Student cancels their booking (optional)

### 4. Admin Management

- **Get All Reservations** - Admin views all bookings
- **Revoke Reservation** - Admin cancels a booking
- **Lock Room** - Admin locks a room (prevents new bookings)
- **Delete Room** - Admin removes a room (cancels all reservations)

### 5. Edge Cases

Test error handling:

- **Double Booking Attempt** - Try to book an already reserved time slot
- **Reserve Locked Room** - Try to book a locked room
- **Unauthorized Access** - Access endpoints without authentication
- **Invalid Time Range** - Submit reservation with end time before start time

## Tips

- Run requests in order the first time to ensure IDs are properly set
- Check the **Tests** tab in each request to see how variables are populated
- The collection handles token management automatically
- You can modify the request bodies to test different scenarios
- Use **Get All Faculties**, **Get All Rooms**, etc. to verify state

## Troubleshooting

**401 Unauthorized**: Token might have expired. Re-run the login or register request.

**404 Not Found**: Make sure you've run the prerequisite requests (e.g., create faculty before creating room).

**400 Bad Request**: Check the request body format and ensure required fields are present.

## Testing All Endpoints

You can use Postman's **Collection Runner** to execute all requests in sequence:

1. Right-click the collection
2. Select **Run collection**
3. Adjust the order if needed
4. Click **Run**

This will execute the entire workflow and show you pass/fail results for each request.
