# Room & Reservation Demo Flow

This document describes an end-to-end flow for managing faculties, rooms, and reservations using the **faculty-service** and **reservation-service**.

---

## 0. Set up environment variables

Paste and adjust these in your shell before running the commands:

```bash
ADMIN_TOKEN=""
STUDENT_TOKEN=""
STUDENT_USER_ID=""
RESERVATION_ID=""
```

---

## 1. Login as admin (faculty-service)

This returns a JWT token. Extract it and save as `ADMIN_TOKEN`.

```bash
curl -X POST http://localhost:8083/auth/login   -H "Content-Type: application/json"   -d '{
    "username": "admin",
    "password": "admin"
  }' | jq '.token'
```

After running this, copy the token (without quotes) and set:

```bash
ADMIN_TOKEN="<paste_token_here>"
```

---

## 2. Create a faculty (admin token required, faculty-service)

```bash
curl -X POST http://localhost:8083/faculties   -H "Content-Type: application/json"   -H "Authorization: Bearer $ADMIN_TOKEN"   -d '{
    "facultyId": "ENGR",
    "name": "Engineering"
  }'
```

---

## 3. Create a room in a faculty (admin token required, faculty-service)

This triggers scheduling-service integration to create the corresponding room.

```bash
curl -X POST http://localhost:8083/rooms   -H "Content-Type: application/json"   -H "Authorization: Bearer $ADMIN_TOKEN"   -d '{
    "roomId": "ENGR-LAB-A101",
    "facultyId": "ENGR",
    "capacity": 50,
    "equipment": "projector,whiteboard,computers"
  }'
```

---

## 4. Student registration and login (faculty-service)

### 4.1 Register a student

```bash
curl -X POST http://localhost:8083/auth/register   -H "Content-Type: application/json"   -d '{
    "username": "student_john",
    "password": "password123",
    "role": "STUDENT",
    "facultyId": "ENGR"
  }'
```

### 4.2 Login as student

Save the returned token as `STUDENT_TOKEN`.

```bash
curl -X POST http://localhost:8083/auth/login   -H "Content-Type: application/json"   -d '{
    "username": "student_john",
    "password": "password123"
  }' | jq '.token'
```

After running, copy the token and set:

```bash
STUDENT_TOKEN="<paste_token_here>"
```

---

## 5. Create a reservation (student token required, reservation-service)

Use future dates in ISO 8601 format (`YYYY-MM-DDTHH:mm:ss`).

Example below uses Dec 15, 2025 at 10:00–12:00.

```bash
curl -X POST http://localhost:8081/reservations   -H "Content-Type: application/json"   -H "Authorization: Bearer $STUDENT_TOKEN"   -d '{
    "roomId": "ENGR-LAB-A101",
    "startDateTime": "2025-12-15T10:00:00",
    "endDateTime": "2025-12-15T12:00:00",
    "attendees": 25,
    "equipment": ["projector"]
  }' | jq '.id'
```

Save the returned id as:

```bash
RESERVATION_ID="<paste_id_here>"
```

---

## 6. Admin reservation management (reservation-service)

### 6.1 List all reservations

```bash
curl -X GET http://localhost:8081/reservations   -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'
```

### 6.2 Approve a reservation

Use the `RESERVATION_ID` from step 5.

```bash
curl -X POST http://localhost:8081/reservations/$RESERVATION_ID/approve   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.3 Revoke/cancel a single reservation

Changes reservation status to `REVOKED`.

```bash
curl -X POST http://localhost:8081/reservations/$RESERVATION_ID/revoke   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 6.4 Revoke all reservations for a specific room

This is typically called internally when a room is deleted or locked.  
It revokes all active reservations for a given `roomId`.

```bash
curl -X DELETE http://localhost:8081/reservations/by-room/ENGR-LAB-A101   -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 7. Room lifecycle operations (faculty-service)

### 7.1 Lock a room

Prevents new reservations and clears existing schedules in scheduling-service.

```bash
curl -X POST http://localhost:8083/rooms/ENGR-LAB-A101/lock   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 7.2 Delete a room

Removes the room and triggers cleanup in scheduling-service.  
Also revokes all reservations via reservation-service.

```bash
curl -X DELETE http://localhost:8083/rooms/ENGR-LAB-A101   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 7.3 Unlock a room (optional, for testing)

```bash
curl -X POST http://localhost:8083/rooms/ENGR-LAB-A101/unlock   -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 8. Additional useful endpoints

### 8.1 Get my reservations (student)

Returns all reservations created by the authenticated student.

```bash
curl -X GET http://localhost:8081/reservations/me   -H "Authorization: Bearer $STUDENT_TOKEN"
```

### 8.2 Get a single reservation

Admins can view any reservation; students can only view their own.

```bash
curl -X GET http://localhost:8081/reservations/$RESERVATION_ID   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 8.3 List all rooms

Returns all rooms (auth required).

```bash
curl -X GET http://localhost:8083/rooms   -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 8.4 List all faculties

```bash
curl -X GET http://localhost:8083/faculties   -H "Authorization: Bearer $ADMIN_TOKEN"
```
