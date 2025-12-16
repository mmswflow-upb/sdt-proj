# Unit Tests

## Overview

We implemented unit tests for core service layer components across all microservices using JUnit 5 and Mockito.

## Test Coverage

### Reservation Service

- `createReservation_Success()` - Validates reservation creation with available room
- `createReservation_ThrowsException_WhenStartTimeIsInThePast()` - Prevents past reservations
- `createReservation_ThrowsException_WhenDuplicateReservationExists()` - Prevents double booking
- `createReservation_ThrowsException_WhenRoomNotAvailable()` - Checks room availability
- `approveReservation_Success()` - Approves pending reservations
- `approveReservation_ThrowsException_WhenReservationCancelled()` - Prevents approving cancelled reservations
- `cancelReservation_Success()` - Cancels reservations
- `cancelReservation_ThrowsException_WhenUserDoesNotOwnReservation()` - Enforces ownership
- `revokeReservation_Success()` - Revokes approved reservations
- `getReservationsForUser_ReturnsUserReservations()` - Retrieves user reservations
- `getReservationsForUserByStatus_FiltersCorrectly()` - Filters by status

### Faculty Service

#### UserServiceTest

- `register_Success_WithDefaultStudentRole()` - Registers users with default student role
- `register_Success_WithAdminRole()` - Registers users with specified roles
- `register_ThrowsException_WhenUsernameAlreadyExists()` - Prevents duplicate usernames
- `register_ThrowsException_WhenInvalidRole()` - Validates role values
- `register_EncodesPasswordCorrectly()` - Ensures password encoding
- `findByUsername_ReturnsUser_WhenUserExists()` - Retrieves users by username

#### FacultyServiceTest

- `createFaculty_Success()` - Creates new faculties
- `createFaculty_ThrowsException_WhenFacultyAlreadyExists()` - Prevents duplicate faculties
- `updateFaculty_Success()` - Updates faculty information
- `deleteFaculty_Success_DeletesAssociatedRooms()` - Cascades room deletion
- `deleteFaculty_ContinuesAfterRoomDeletionFailure()` - Handles deletion errors
- `getFaculty_ReturnsData_WhenFacultyExists()` - Retrieves faculty data
- `getAllFaculties_ReturnsAllFaculties()` - Lists all faculties

### Scheduling Service

- `isAvailable_ReturnsTrue_WhenNoSchedulesExist()` - Checks availability for empty schedules
- `isAvailable_ReturnsFalse_WhenSchedulesOverlap()` - Detects schedule conflicts
- `isAvailable_ReturnsTrue_WhenEndTimeEqualsExistingStartTime()` - Handles boundary cases
- `isAvailable_HandlesMultipleSchedules()` - Validates against multiple schedules
- `createSchedule_Success()` - Creates new schedules
- `createSchedule_ThrowsException_WhenRoomNotFound()` - Validates room existence
- `removeSchedulesForRoom_Success()` - Removes schedules by room
- `removeSchedulesForReservation_Success()` - Removes schedules by reservation

## Running Tests

Tests run automatically during Docker image build:
```bash
docker-compose build
```

To run tests locally without Docker, Maven must be installed:
```bash
cd reservation-service
mvn test
```

Test results are in `target/surefire-reports/` for each service.

## Mocked Components

**Reservation Service**: ReservationRepository, SchedulingClient, NotificationPublisher  
**User Service**: UserRepository, PasswordEncoder  
**Faculty Service**: FacultyRepository, FacultyRoomRepository, FacultyRoomService  
**Scheduling Service**: RoomRepository, RoomScheduleRepository

## CI/CD Integration

Unit tests are part of our CI/CD pipeline. If any test fails during the Docker build, deployment stops immediately.
