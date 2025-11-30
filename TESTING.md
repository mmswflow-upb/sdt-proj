# STEP 0: Set up environment variables (paste these first)
ADMIN_TOKEN=""
STUDENT_TOKEN=""
STUDENT_USER_ID=""
RESERVATION_ID=""

# STEP 1: Login as admin (faculty-service)
# This returns a JWT token. Extract it and save as ADMIN_TOKEN.

curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }' | jq '.token'

# After running this, copy the token (without quotes) and run:
# ADMIN_TOKEN="<paste_token_here>"


# STEP 2: Create a faculty (admin token required, faculty-service)

curl -X POST http://localhost:8083/faculties \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "facultyId": "ENGR",
    "name": "Engineering"
  }'


# STEP 3: Create a room in a faculty (admin token required, faculty-service)
# This triggers scheduling-service integration to create the corresponding room.

curl -X POST http://localhost:8083/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "roomId": "ENGR-LAB-A101",
    "facultyId": "ENGR",
    "capacity": 50,
    "equipment": "projector,whiteboard,computers"
  }'


# STEP 4a: Register a student (faculty-service)

curl -X POST http://localhost:8083/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student_john",
    "password": "password123",
    "role": "STUDENT",
    "facultyId": "ENGR"
  }'


# STEP 4b: Login as student (faculty-service)
# Save the returned token as STUDENT_TOKEN

curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student_john",
    "password": "password123"
  }' | jq '.token'

# After running, copy the token and run:
# STUDENT_TOKEN="<paste_token_here>"


# STEP 5: Create a reservation (student token required, reservation-service)
# Note: Use future dates in ISO 8601 format (YYYY-MM-DDTHH:mm:ss)
# Example below uses Dec 15, 2025 at 10:00 AM to 12:00 PM

curl -X POST http://localhost:8081/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "roomId": "ENGR-LAB-A101",
    "startDateTime": "2025-12-15T10:00:00",
    "endDateTime": "2025-12-15T12:00:00",
    "attendees": 25,
    "equipment": ["projector"]
  }' | jq '.id'

# Save the returned id as RESERVATION_ID
# RESERVATION_ID="<paste_id_here>"


# STEP 6a: List all reservations (admin token required, reservation-service)

curl -X GET http://localhost:8081/reservations \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'


# STEP 6b: Approve a reservation (admin token required, reservation-service)
# Use the RESERVATION_ID from STEP 5

curl -X POST http://localhost:8081/reservations/$RESERVATION_ID/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# STEP 7: Revoke/cancel a single reservation (admin token required, reservation-service)
# Revokes the reservation (changes status to REVOKED)

curl -X POST http://localhost:8081/reservations/$RESERVATION_ID/revoke \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# STEP 8: Revoke all reservations for a specific room (admin token required, reservation-service)
# This endpoint is called internally when a room is deleted or locked
# It revokes all active reservations for a given roomId

curl -X DELETE http://localhost:8081/reservations/by-room/ENGR-LAB-A101 \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# STEP 9a: Lock a room (admin token required, faculty-service)
# Prevents new reservations and clears existing schedules in scheduling-service

curl -X POST http://localhost:8083/rooms/ENGR-LAB-A101/lock \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# STEP 9b: Delete a room (admin token required, faculty-service)
# Removes the room and triggers cleanup in scheduling-service
# Also revokes all reservations via reservation-service

curl -X DELETE http://localhost:8083/rooms/ENGR-LAB-A101 \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# ADDITIONAL: Unlock a room (admin token required, faculty-service)
# (Not part of main flow but useful for testing)

curl -X POST http://localhost:8083/rooms/ENGR-LAB-A101/unlock \
  -H "Authorization: Bearer $ADMIN_TOKEN"


# ADDITIONAL: Get my reservations (student token, reservation-service)
# Returns all reservations created by the authenticated student

curl -X GET http://localhost:8081/reservations/me \
  -H "Authorization: Bearer $STUDENT_TOKEN"

# ADDITIONAL: Get a single reservation (reservation-service)
# Admins can view any reservation; students can only view their own

curl -X GET http://localhost:8081/reservations/$RESERVATION_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# ADDITIONAL: List all rooms (faculty-service)
# Returns all rooms (public endpoint, but auth still required)

curl -X GET http://localhost:8083/rooms \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# ADDITIONAL: List all faculties (faculty-service)

curl -X GET http://localhost:8083/faculties \
  -H "Authorization: Bearer $ADMIN_TOKEN"