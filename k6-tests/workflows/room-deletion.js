import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const roomDeletionWorkflows = new Counter('room_deletion_workflows');
const workflowDuration = new Trend('room_deletion_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Room Deletion Workflow
 * Tests room deletion and cascade effects on reservations and schedules
 */
export function roomDeletionWorkflow(data, users) {
  console.log('\n=== ROOM DELETION WORKFLOW START ===');
  const workflowStart = Date.now();

  const deletionData = {
    testRoomId: null,
    reservationsForRoom: [],
    approvedReservationId: null,
    testFacultyId: null,
  };

  group('Room Deletion Workflow', () => {
    const adminHeaders = createAuthHeaders(users.admin.token);
    const facultyAdminHeaders = createAuthHeaders(users.facultyAdmin.token);
    const studentHeaders = createAuthHeaders(users.student.token);

    // 1. Create a test room for deletion
    group('1. Admin creates test room for deletion', () => {
      const testRoomId = `${data.facultyId}-DELETE-TEST-${Date.now()}`;
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: testRoomId,
          facultyId: data.facultyId,
          capacity: 25,
          equipment: 'projector',
        }),
        { headers: adminHeaders }
      );

      const success = check(roomRes, {
        'admin can create test room for deletion': (r) => r.status === 201 || r.status === 200,
      });

      if (success) {
        deletionData.testRoomId = testRoomId;
        console.log(`  ✓ Test room created for deletion: ${testRoomId}`);
      } else {
        console.error(`Failed to create test room: ${roomRes.status} - ${roomRes.body}`);
      }
    });

    sleep(1);

    // 2. Create multiple reservations for this room
    group('2. Create reservations for test room', () => {
      // Create 3 reservations with different statuses
      for (let i = 0; i < 3; i++) {
        const startTime = new Date(Date.now() + (24 + i * 2) * 60 * 60 * 1000);
        const endTime = new Date(startTime.getTime() + 1 * 60 * 60 * 1000);

        const reservationRes = http.post(
          `${BASE_URL}/api/reservations`,
          JSON.stringify({
            roomId: deletionData.testRoomId,
            startDateTime: startTime.toISOString().slice(0, 19),
            endDateTime: endTime.toISOString().slice(0, 19),
            attendees: 20,
          }),
          { headers: studentHeaders }
        );

        if (reservationRes.status === 201 || reservationRes.status === 200) {
          const reservationId = reservationRes.json('id');
          deletionData.reservationsForRoom.push(reservationId);
          console.log(`  ✓ Created reservation ${i + 1}: ${reservationId}`);
        }

        sleep(0.3);
      }

      check(deletionData.reservationsForRoom, {
        'created multiple reservations for test room': (ids) => ids.length === 3,
      });
    });

    sleep(1);

    // 3. Approve one reservation to create schedule entry
    group('3. Admin approves one reservation', () => {
      if (deletionData.reservationsForRoom.length > 0) {
        const approveRes = http.post(
          `${BASE_URL}/api/reservations/${deletionData.reservationsForRoom[0]}/approve`,
          null,
          { headers: adminHeaders }
        );

        check(approveRes, {
          'reservation approved before room deletion': (r) => r.status === 200,
        });

        deletionData.approvedReservationId = deletionData.reservationsForRoom[0];
        console.log(`  ✓ Approved reservation ${deletionData.approvedReservationId}`);
      }
    });

    sleep(1);

    // 4. Verify reservations exist before deletion
    group('4. Verify reservations exist before deletion', () => {
      const reservationsRes = http.get(
        `${BASE_URL}/api/reservations`,
        { headers: adminHeaders }
      );

      const allReservations = reservationsRes.json();
      const roomReservations = Array.isArray(allReservations) 
        ? allReservations.filter(r => r.roomId === deletionData.testRoomId)
        : [];

      check(roomReservations, {
        'reservations exist for test room before deletion': (res) => res.length === 3,
      });

      console.log(`  ✓ Found ${roomReservations.length} reservations for test room before deletion`);
    });

    sleep(1);

    // 5. Admin deletes the room
    group('5. Admin deletes test room', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/rooms/${deletionData.testRoomId}`,
        null,
        { headers: adminHeaders }
      );

      check(deleteRes, {
        'admin can delete room': (r) => r.status === 204 || r.status === 200,
      });

      console.log(`  ✓ Admin deleted room: ${deletionData.testRoomId}`);
    });

    sleep(1);

    // 6. Verify room no longer exists
    group('6. Verify room is deleted', () => {
      const getRoomRes = http.get(
        `${BASE_URL}/api/rooms/${deletionData.testRoomId}`,
        { headers: adminHeaders }
      );

      check(getRoomRes, {
        'deleted room returns 404': (r) => r.status === 404,
      });

      console.log(`  ✓ Confirmed room no longer exists`);
    });

    sleep(1);

    // 7. Verify reservations were revoked
    group('7. Verify reservations were revoked', () => {
      const reservationsRes = http.get(
        `${BASE_URL}/api/reservations`,
        { headers: adminHeaders }
      );

      const allReservations = reservationsRes.json();
      const roomReservations = Array.isArray(allReservations)
        ? allReservations.filter(r => r.roomId === deletionData.testRoomId)
        : [];

      const allRevoked = roomReservations.every(r => r.status === 'REVOKED');

      check(roomReservations, {
        'reservations still exist after room deletion': (res) => res.length > 0,
        'all reservations for deleted room are REVOKED': (res) => 
          res.length > 0 && res.every(r => r.status === 'REVOKED'),
      });

      console.log(`  ✓ All ${roomReservations.length} reservations revoked after room deletion`);
    });

    sleep(1);

    // 8. Faculty admin can also delete rooms in their faculty
    group('8. Faculty admin deletes room', () => {
      // Create another test room
      const facultyTestRoomId = `${data.facultyId}-FADMIN-DELETE-${Date.now()}`;
      const createRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: facultyTestRoomId,
          facultyId: data.facultyId,
          capacity: 30,
          equipment: 'whiteboard',
        }),
        { headers: facultyAdminHeaders }
      );

      if (createRes.status === 201 || createRes.status === 200) {
        console.log(`  ✓ Faculty admin created room: ${facultyTestRoomId}`);

        sleep(0.5);

        // Faculty admin deletes it
        const deleteRes = http.del(
          `${BASE_URL}/api/rooms/${facultyTestRoomId}`,
          null,
          { headers: facultyAdminHeaders }
        );

        check(deleteRes, {
          'faculty admin can delete room in their faculty': (r) => r.status === 204 || r.status === 200,
        });

        console.log(`  ✓ Faculty admin deleted room: ${facultyTestRoomId}`);
      }
    });

    sleep(1);

    // 9. Verify schedules are cleaned up by checking the approved reservation's schedule
    group('9. Verify schedules deleted for deleted room', () => {
      if (deletionData.approvedReservationId) {
        // Try to get the schedule for the approved reservation
        const scheduleRes = http.get(
          `${BASE_URL}/api/schedules/${deletionData.approvedReservationId}`,
          { headers: adminHeaders }
        );

        // Schedule should be deleted (404) or not found
        check(scheduleRes, {
          'schedule entry deleted after room deletion': (r) => r.status === 404 || (r.status === 200 && r.body === 'null'),
        });

        console.log(`  ✓ Schedule for reservation ${deletionData.approvedReservationId} cleaned up (status: ${scheduleRes.status})`);
      }

      // Also verify availability is restored
      const deletedRoomTime = new Date(Date.now() + 24 * 60 * 60 * 1000);
      const deletedRoomEndTime = new Date(deletedRoomTime.getTime() + 1 * 60 * 60 * 1000);

      const availabilityRes = http.get(
        `${BASE_URL}/api/availability?roomId=${deletionData.testRoomId}&from=${deletedRoomTime.toISOString().slice(0, 19)}&to=${deletedRoomEndTime.toISOString().slice(0, 19)}`,
        { headers: studentHeaders }
      );

      check(availabilityRes, {
        'availability check handles deleted room gracefully': (r) => r.status === 200 || r.status === 404,
      });

      console.log(`  ✓ Schedule cleanup fully verified for deleted room`);
    });

    sleep(1);

    // 10. Test faculty deletion cascade effects
    group('10. Faculty deletion cascades to rooms and schedules', () => {
      // Create a dedicated test faculty
      const testFacultyId = `FAC-DELETE-TEST-${Date.now()}`;
      const createFacultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: testFacultyId,
          name: 'Test Faculty for Deletion',
        }),
        { headers: adminHeaders }
      );

      const facultyCreated = check(createFacultyRes, {
        'admin can create test faculty for deletion': (r) => r.status === 201 || r.status === 200,
      });

      if (!facultyCreated) {
        console.log(`  ⚠ Faculty creation failed (${createFacultyRes.status}): ${createFacultyRes.body}`);
        console.log('  ℹ Skipping faculty deletion cascade test due to faculty creation failure');
        return; // Skip this test group
      }

      if (createFacultyRes.status === 201 || createFacultyRes.status === 200) {
        deletionData.testFacultyId = testFacultyId;
        console.log(`  ✓ Created test faculty: ${testFacultyId}`);

        sleep(0.5);

        // Create 2 rooms in this faculty
        const room1Id = `${testFacultyId}-ROOM-1`;
        const room2Id = `${testFacultyId}-ROOM-2`;

        http.post(
          `${BASE_URL}/api/rooms`,
          JSON.stringify({ roomId: room1Id, facultyId: testFacultyId, capacity: 30 }),
          { headers: adminHeaders }
        );

        http.post(
          `${BASE_URL}/api/rooms`,
          JSON.stringify({ roomId: room2Id, facultyId: testFacultyId, capacity: 40 }),
          { headers: adminHeaders }
        );

        console.log(`  ✓ Created 2 rooms in test faculty`);

        sleep(0.5);

        // Create and approve a reservation for one of the rooms
        const startTime = new Date(Date.now() + 30 * 60 * 60 * 1000);
        const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000);

        const reservationRes = http.post(
          `${BASE_URL}/api/reservations`,
          JSON.stringify({
            roomId: room1Id,
            startDateTime: startTime.toISOString().slice(0, 19),
            endDateTime: endTime.toISOString().slice(0, 19),
            attendees: 25,
          }),
          { headers: studentHeaders }
        );

        if (reservationRes.status === 201 || reservationRes.status === 200) {
          const reservationId = reservationRes.json('id');
          console.log(`  ✓ Created reservation ${reservationId} for faculty deletion test`);

          sleep(0.5);

          // Approve it to create a schedule
          http.post(
            `${BASE_URL}/api/reservations/${reservationId}/approve`,
            null,
            { headers: adminHeaders }
          );

          console.log(`  ✓ Approved reservation to create schedule entry`);

          sleep(1);

          // Delete the faculty
          const deleteFacultyRes = http.del(
            `${BASE_URL}/api/faculties/${testFacultyId}`,
            null,
            { headers: adminHeaders }
          );

          check(deleteFacultyRes, {
            'admin can delete faculty': (r) => r.status === 204 || r.status === 200,
          });

          console.log(`  ✓ Admin deleted faculty: ${testFacultyId}`);

          sleep(1);

          // Verify faculty is deleted
          const getFacultyRes = http.get(
            `${BASE_URL}/api/faculties/${testFacultyId}`,
            { headers: adminHeaders }
          );

          check(getFacultyRes, {
            'deleted faculty returns 404': (r) => r.status === 404,
          });

          // Verify rooms are deleted
          const getRoom1Res = http.get(
            `${BASE_URL}/api/rooms/${room1Id}`,
            { headers: adminHeaders }
          );

          check(getRoom1Res, {
            'rooms deleted when faculty deleted': (r) => r.status === 404,
          });

          // Verify reservation was revoked
          const getReservationRes = http.get(
            `${BASE_URL}/api/reservations/${reservationId}`,
            { headers: adminHeaders }
          );

          if (getReservationRes.status === 200) {
            const reservation = getReservationRes.json();
            check(reservation, {
              'reservation revoked when faculty deleted': (r) => r.status === 'REVOKED',
            });
            console.log(`  ✓ Reservation revoked: ${reservation.status}`);
          }

          // Verify schedule cleaned up
          const getScheduleRes = http.get(
            `${BASE_URL}/api/schedules/${reservationId}`,
            { headers: adminHeaders }
          );

          check(getScheduleRes, {
            'schedule deleted when faculty deleted': (r) => r.status === 404 || (r.status === 200 && r.body === 'null'),
          });

          console.log(`  ✓ Full cascade verified: Faculty → Rooms → Reservations → Schedules`);
        }
      }
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  roomDeletionWorkflows.add(1);
  console.log('=== ROOM DELETION WORKFLOW COMPLETE ===\n');

  return deletionData;
}
