import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const facultyAdminWorkflows = new Counter('faculty_admin_workflows');
const unauthorizedBlocked = new Counter('unauthorized_blocked');
const workflowDuration = new Trend('workflow_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'faculty_admin_workflows': ['count>0'],
    'unauthorized_blocked': ['count>0'],
  },
};

export function facultyAdminWorkflow(data) {
  console.log('\n=== FACULTY ADMIN WORKFLOW START ===');
  const workflowStart = Date.now();

  group('Faculty Admin Workflow', () => {
    const username = `faculty_admin_${Date.now()}`;

    // 1. Register as faculty admin
    let facultyAdminToken;
    group('1. Register faculty admin', () => {
      const registerRes = http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: username,
          password: 'admin123',
          role: 'FACULTY_ADMIN',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const success = check(registerRes, {
        'faculty admin registered': (r) => r.status === 201 || r.status === 200,
      });

      if (!success) {
        console.error(`Faculty admin registration failed: ${registerRes.status}`);
        console.error(`Response: ${registerRes.body}`);
      }
    });

    sleep(1);

    // 2. Login as faculty admin
    group('2. Faculty admin login', () => {
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: username,
          password: 'admin123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const success = check(loginRes, {
        'faculty admin login successful': (r) => r.status === 200,
        'received token': (r) => r.json('token') !== undefined,
      });

      if (!success) {
        console.error(`Faculty admin login failed: ${loginRes.status}`);
        console.error(`Response: ${loginRes.body}`);
      }

      facultyAdminToken = loginRes.json('token');
    });

    if (!facultyAdminToken) {
      console.error('Faculty admin login failed, skipping rest of workflow');
      return;
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${facultyAdminToken}`,
    };

    sleep(1);

    // 3. List rooms (should only see their faculty's rooms)
    group('3. Faculty admin lists rooms', () => {
      const roomsRes = http.get(`${BASE_URL}/api/rooms`, { headers });
      check(roomsRes, {
        'faculty admin can list rooms': (r) => r.status === 200,
      });
    });

    sleep(1);

    // 4. Faculty admin creates a room in their faculty
    let createdRoomId;
    group('4. Faculty admin creates room', () => {
      createdRoomId = `${data.facultyId}_FACADMIN_ROOM_${Date.now()}`;
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: createdRoomId,
          facultyId: data.facultyId,
          capacity: 40,
          equipment: 'projector,whiteboard',
        }),
        { headers }
      );

      const success = check(roomRes, {
        'faculty admin can create room': (r) => r.status === 201 || r.status === 200,
      });

      if (!success) {
        console.error(`Faculty admin room creation failed: ${roomRes.status}`);
        console.error(`Response: ${roomRes.body}`);
      }
    });

    sleep(1);

    // 5. View reservations
    group('5. Faculty admin views reservations', () => {
      const reservationsRes = http.get(`${BASE_URL}/api/reservations`, { headers });
      const success = check(reservationsRes, {
        'faculty admin can view reservations': (r) => r.status === 200,
      });
      
      if (!success) {
        console.error(`Faculty admin view reservations failed: ${reservationsRes.status}`);
        console.error(`Response: ${reservationsRes.body}`);
      }
    });

    sleep(1);

    // 6. Create a student and their reservation for faculty admin to approve
    const studentUsername = `test_student_for_facadmin_${Date.now()}`;
    let studentToken, testReservationId;

    group('6. Setup: Create student and pending reservation', () => {
      // Register student
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: studentUsername,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login as student
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: studentUsername,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      studentToken = loginRes.json('token');

      sleep(0.5);

      // Create reservation
      const reservationTime = new Date(Date.now() + 96 * 60 * 60 * 1000); // 4 days ahead
      const endReservationTime = new Date(reservationTime.getTime() + 2 * 60 * 60 * 1000);

      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: reservationTime.toISOString().slice(0, 19),
          endDateTime: endReservationTime.toISOString().slice(0, 19),
          attendees: 15,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${studentToken}`,
          },
        }
      );

      testReservationId = reservationRes.json('id');
    });

    sleep(1);

    // 7. Faculty admin approves the reservation
    group('7. Faculty admin approves reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/approve`,
        null,
        { headers }
      );

      const success = check(approveRes, {
        'faculty admin can approve reservation': (r) => r.status === 200,
        'status changed to APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      if (!success) {
        console.error(`Faculty admin approval failed: ${approveRes.status}`);
        console.error(`Response: ${approveRes.body}`);
      }
    });

    sleep(1);

    // 8. Faculty admin revokes the approved reservation
    group('8. Faculty admin revokes approved reservation', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/revoke`,
        null,
        { headers }
      );

      const success = check(revokeRes, {
        'faculty admin can revoke approved reservation': (r) => r.status === 200,
        'status changed to REVOKED': (r) => r.json('status') === 'REVOKED',
      });

      if (!success) {
        console.error(`Faculty admin revoke failed: ${revokeRes.status}`);
        console.error(`Response: ${revokeRes.body}`);
      }
    });

    sleep(1);

    // 9. Faculty admin re-approves the revoked reservation
    group('9. Faculty admin re-approves revoked reservation', () => {
      const reapproveRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/approve`,
        null,
        { headers }
      );

      const success = check(reapproveRes, {
        'faculty admin can re-approve revoked reservation': (r) => r.status === 200,
        'status changed back to APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      if (!success) {
        console.error(`Faculty admin re-approval failed: ${reapproveRes.status}`);
        console.error(`Response: ${reapproveRes.body}`);
      }
    });

    sleep(1);

    // 10. Try to create faculty (should fail - only full admin can)
    group('10. Faculty admin tries to create faculty (should fail)', () => {
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: 'SHOULD_FAIL',
          name: 'Should Not Be Created',
        }),
        { headers }
      );

      check(facultyRes, {
        'faculty admin cannot create faculty': (r) => r.status === 403 || r.status === 401,
      });

      if (facultyRes.status === 403 || facultyRes.status === 401) {
        unauthorizedBlocked.add(1);
      }
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  facultyAdminWorkflows.add(1);
  console.log('=== FACULTY ADMIN WORKFLOW COMPLETE ===\n');
}
