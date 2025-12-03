import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const studentWorkflows = new Counter('student_workflows');
const unauthorizedBlocked = new Counter('unauthorized_blocked');
const workflowDuration = new Trend('workflow_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'student_workflows': ['count>0'],
  },
};

export function studentWorkflow(data) {
  console.log('\n=== STUDENT WORKFLOW START ===');
  const workflowStart = Date.now();

  group('Student Workflow', () => {
    const username = `student_${__VU}_${Date.now()}`;

    // 1. Register as student
    let studentToken;
    group('1. Student registration', () => {
      const registerRes = http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: username,
          password: 'student123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      check(registerRes, {
        'student registered': (r) => r.status === 201 || r.status === 200,
      });
    });

    sleep(1);

    // 2. Login as student
    group('2. Student login', () => {
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: username,
          password: 'student123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      check(loginRes, {
        'student login successful': (r) => r.status === 200,
        'received token': (r) => r.json('token') !== undefined,
      });

      studentToken = loginRes.json('token');
    });

    if (!studentToken) {
      console.error('Student login failed, skipping rest of workflow');
      return;
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${studentToken}`,
    };

    sleep(1);

    // 3. Check room availability
    const roomId = data.rooms[0];
    const startTime = new Date(Date.now() + 48 * 60 * 60 * 1000); // 2 days ahead
    const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000);

    group('3. Check availability', () => {
      const availabilityRes = http.get(
        `${BASE_URL}/api/availability?roomId=${roomId}&from=${startTime.toISOString().slice(0, 19)}&to=${endTime.toISOString().slice(0, 19)}`,
        { headers }
      );

      check(availabilityRes, {
        'student can check availability': (r) => r.status === 200,
      });
    });

    sleep(1);

    // 4. Create reservation
    let reservationId;
    group('4. Student creates reservation', () => {
      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: startTime.toISOString().slice(0, 19),
          endDateTime: endTime.toISOString().slice(0, 19),
          attendees: 10,
        }),
        { headers }
      );

      const success = check(reservationRes, {
        'student can create reservation': (r) => r.status === 201 || r.status === 200,
        'reservation has ID': (r) => r.json('id') !== undefined,
      });

      if (!success) {
        console.error(`Student reservation creation failed: ${reservationRes.status}`);
        console.error(`Response: ${reservationRes.body}`);
      }

      reservationId = reservationRes.json('id');
    });

    sleep(1);

    // 5. View own reservations
    group('5. Student views own reservations', () => {
      const myReservationsRes = http.get(`${BASE_URL}/api/reservations/me`, { headers });
      
      check(myReservationsRes, {
        'student can view own reservations': (r) => r.status === 200,
        'reservation list contains new reservation': (r) => {
          const reservations = r.json();
          return Array.isArray(reservations) && reservations.some(res => res.id === reservationId);
        },
        'reservation is PENDING': (r) => {
          const reservations = r.json();
          const reservation = reservations.find(res => res.id === reservationId);
          return reservation && reservation.status === 'PENDING';
        },
      });
    });

    sleep(1);

    // 6. Try to approve own reservation (should fail)
    group('6. Student tries to approve own reservation (should fail)', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${reservationId}/approve`,
        null,
        { headers }
      );

      const success = check(approveRes, {
        'student cannot approve own reservation': (r) => r.status === 403 || r.status === 401,
      });

      if (!success) {
        console.error(`Student approve check failed: ${approveRes.status} - ${approveRes.body}`);
      } else if (approveRes.status === 403 || approveRes.status === 401) {
        unauthorizedBlocked.add(1);
      }
    });

    sleep(1);

    // 7. Admin approves the reservation
    group('7. Admin approves student reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${reservationId}/approve`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${data.adminToken}`,
          },
        }
      );

      const success = check(approveRes, {
        'admin can approve reservation': (r) => r.status === 200,
        'status changed to APPROVED': (r) => r.json('status') === 'APPROVED',
      });
      
      if (!success) {
        console.error(`Admin approve failed: ${approveRes.status} - ${approveRes.body}`);
      }
    });

    sleep(1);

    // 8. Verify approval
    group('8. Student verifies approval', () => {
      const verifyRes = http.get(
        `${BASE_URL}/api/reservations/${reservationId}`,
        { headers }
      );

      check(verifyRes, {
        'student can view approved reservation': (r) => r.status === 200,
        'status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });
    });

    sleep(1);

    // 9. Student tries to revoke own reservation (should fail)
    group('9. Student tries to revoke own reservation (should fail)', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${reservationId}/revoke`,
        null,
        { headers }
      );

      const success = check(revokeRes, {
        'student cannot revoke own reservation': (r) => r.status === 403 || r.status === 401,
      });

      if (success && (revokeRes.status === 403 || revokeRes.status === 401)) {
        unauthorizedBlocked.add(1);
      } else {
        console.error(`Student revoke should have failed but got: ${revokeRes.status}`);
      }
    });

    sleep(1);

    // 10. Admin revokes the reservation
    group('10. Admin revokes student reservation', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${reservationId}/revoke`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${data.adminToken}`,
          },
        }
      );

      check(revokeRes, {
        'admin can revoke reservation': (r) => r.status === 200,
        'status changed to REVOKED': (r) => r.json('status') === 'REVOKED',
      });
    });

    sleep(1);

    // 11. Student views revoked reservation
    group('11. Student views revoked reservation', () => {
      const revokedRes = http.get(
        `${BASE_URL}/api/reservations/${reservationId}`,
        { headers }
      );

      check(revokedRes, {
        'student can view revoked reservation': (r) => r.status === 200,
        'status is REVOKED': (r) => r.json('status') === 'REVOKED',
      });
    });

    sleep(1);

    // 12. Test cancel functionality: create, approve, cancel, then re-book same slot
    let cancelTestReservationId;
    const cancelTestTime = new Date(Date.now() + 192 * 60 * 60 * 1000); // 8 days ahead
    const cancelTestEndTime = new Date(cancelTestTime.getTime() + 2 * 60 * 60 * 1000);
    
    group('12. Student creates reservation for cancel test', () => {
      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: cancelTestTime.toISOString().slice(0, 19),
          endDateTime: cancelTestEndTime.toISOString().slice(0, 19),
          attendees: 15,
        }),
        { headers }
      );

      const success = check(reservationRes, {
        'cancel test reservation created': (r) => r.status === 201 || r.status === 200,
        'cancel test reservation has ID': (r) => r.json('id') !== undefined,
      });

      if (success) {
        cancelTestReservationId = reservationRes.json('id');
      }
    });

    sleep(1);

    // 13. Admin approves the cancel test reservation
    group('13. Admin approves cancel test reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${cancelTestReservationId}/approve`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${data.adminToken}`,
          },
        }
      );

      check(approveRes, {
        'cancel test reservation approved': (r) => r.status === 200,
        'cancel test status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });
    });

    sleep(1);

    // 14. Student cancels their own approved reservation
    group('14. Student cancels approved reservation', () => {
      const cancelRes = http.post(
        `${BASE_URL}/api/reservations/${cancelTestReservationId}/cancel`,
        null,
        { headers }
      );

      check(cancelRes, {
        'student can cancel own reservation': (r) => r.status === 200,
        'status changed to CANCELLED': (r) => r.json('status') === 'CANCELLED',
      });
    });

    sleep(1);

    // 15. Verify cancelled reservation cannot be approved by admin
    group('15. Admin tries to approve cancelled reservation (should fail)', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${cancelTestReservationId}/approve`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${data.adminToken}`,
          },
        }
      );

      const success = check(approveRes, {
        'cannot approve cancelled reservation': (r) => r.status === 500 || r.status === 400 || r.status === 409,
      });

      if (!success) {
        console.error(`Admin approve of cancelled should have failed but got: ${approveRes.status}`);
      }
    });

    sleep(1);

    // 16. Student creates new reservation for same room and time slot (should work now)
    group('16. Student re-books same slot after cancellation', () => {
      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: cancelTestTime.toISOString().slice(0, 19),
          endDateTime: cancelTestEndTime.toISOString().slice(0, 19),
          attendees: 15,
        }),
        { headers }
      );

      check(reservationRes, {
        'can rebook same slot after cancellation': (r) => r.status === 201 || r.status === 200,
        'rebooking has new ID': (r) => r.json('id') !== undefined && r.json('id') !== cancelTestReservationId,
      });
    });

    sleep(1);

    // 17. Student cannot cancel another student's reservation
    group('17. Student cannot cancel other student reservations', () => {
      // Create another student to make a reservation
      const otherStudentUsername = `otherstudent_${Date.now()}`;
      const otherStudentRegRes = http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: otherStudentUsername,
          password: 'password123',
          role: 'STUDENT',
          email: `${otherStudentUsername}@university.com`,
          firstName: 'Other',
          lastName: 'Student',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const otherStudentLoginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: otherStudentUsername,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const otherStudentToken = otherStudentLoginRes.json('token');
      const otherStudentHeaders = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${otherStudentToken}`,
      };

      // Other student creates a reservation
      const otherReservationTime = new Date(Date.now() + 200 * 60 * 60 * 1000);
      const otherReservationEndTime = new Date(otherReservationTime.getTime() + 1 * 60 * 60 * 1000);
      
      const otherReservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: otherReservationTime.toISOString().slice(0, 19),
          endDateTime: otherReservationEndTime.toISOString().slice(0, 19),
          attendees: 20,
        }),
        { headers: otherStudentHeaders }
      );

      const otherReservationId = otherReservationRes.json('id');

      // Original student tries to cancel other student's reservation
      const cancelAttemptRes = http.post(
        `${BASE_URL}/api/reservations/${otherReservationId}/cancel`,
        null,
        { headers }
      );

      check(cancelAttemptRes, {
        'cannot cancel other student reservation': (r) => r.status === 403 || r.status === 404,
      });
    });

    sleep(1);

    // 18. Student creates multiple concurrent reservations for different rooms
    group('18. Student creates multiple concurrent reservations', () => {
      const concurrentTime = new Date(Date.now() + 168 * 60 * 60 * 1000); // 7 days ahead
      const concurrentEndTime = new Date(concurrentTime.getTime() + 1 * 60 * 60 * 1000);
      const reservationIds = [];

      // Create reservations for all available rooms
      for (let i = 0; i < data.rooms.length; i++) {
        const reservationRes = http.post(
          `${BASE_URL}/api/reservations`,
          JSON.stringify({
            roomId: data.rooms[i],
            startDateTime: concurrentTime.toISOString().slice(0, 19),
            endDateTime: concurrentEndTime.toISOString().slice(0, 19),
            attendees: 5 + i * 2,
          }),
          { headers }
        );

        const success = check(reservationRes, {
          [`student can create reservation for room ${i + 1}`]: (r) => r.status === 201 || r.status === 200,
          [`reservation ${i + 1} has ID`]: (r) => r.json('id') !== undefined,
        });

        if (success) {
          reservationIds.push(reservationRes.json('id'));
        }
      }

      // Verify all reservations were created
      check(reservationIds, {
        'multiple concurrent reservations created': (ids) => ids.length === data.rooms.length,
      });

      console.log(`Created ${reservationIds.length} concurrent reservations for different rooms`);
    });

    sleep(1);

    // 19. Student tries to create a room (should fail)
    group('19. Student tries to create room (should fail)', () => {
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: `STUDENT_ROOM_${Date.now()}`,
          facultyId: data.facultyId,
          capacity: 30,
          equipment: 'projector',
        }),
        { headers }
      );

      const success = check(roomRes, {
        'student cannot create room': (r) => r.status === 403 || r.status === 401,
      });

      if (success && (roomRes.status === 403 || roomRes.status === 401)) {
        unauthorizedBlocked.add(1);
      } else {
        console.error(`Student room creation should have failed but got: ${roomRes.status}`);
      }
    });

    sleep(1);

    // 20. Student tries to delete a room (should fail)
    group('20. Student tries to delete room (should fail)', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/rooms/${data.rooms[0]}`,
        null,
        { headers }
      );

      const success = check(deleteRes, {
        'student cannot delete room': (r) => r.status === 403 || r.status === 401,
      });

      if (success && (deleteRes.status === 403 || deleteRes.status === 401)) {
        unauthorizedBlocked.add(1);
      } else {
        console.error(`Student room deletion should have failed but got: ${deleteRes.status}`);
      }
    });

    sleep(1);

    // 21. Student tries to create a faculty (should fail)
    group('21. Student tries to create faculty (should fail)', () => {
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: `STUDENT_FAC_${Date.now()}`,
          name: 'Student Created Faculty',
        }),
        { headers }
      );

      const success = check(facultyRes, {
        'student cannot create faculty': (r) => r.status === 403 || r.status === 401,
      });

      if (success && (facultyRes.status === 403 || facultyRes.status === 401)) {
        unauthorizedBlocked.add(1);
      } else {
        console.error(`Student faculty creation should have failed but got: ${facultyRes.status}`);
      }
    });

    sleep(1);

    // 22. Student tries to delete a faculty (should fail)
    group('22. Student tries to delete faculty (should fail)', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/faculties/${data.facultyId}`,
        null,
        { headers }
      );

      const success = check(deleteRes, {
        'student cannot delete faculty': (r) => r.status === 403 || r.status === 401,
      });

      if (success && (deleteRes.status === 403 || deleteRes.status === 401)) {
        unauthorizedBlocked.add(1);
      } else {
        console.error(`Student faculty deletion should have failed but got: ${deleteRes.status}`);
      }
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  studentWorkflows.add(1);
  console.log('=== STUDENT WORKFLOW COMPLETE ===\n');
}
