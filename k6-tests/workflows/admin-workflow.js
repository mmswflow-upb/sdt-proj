import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const adminWorkflows = new Counter('admin_workflows');
const workflowDuration = new Trend('workflow_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'admin_workflows': ['count>0'],
  },
};

export function adminWorkflow(data) {
  console.log('\n=== ADMIN WORKFLOW START ===');
  const workflowStart = Date.now();

  group('Admin Workflow', () => {
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.adminToken}`,
    };

    // 1. Create a new faculty
    group('1. Admin creates faculty', () => {
      const newFacultyId = `ADMIN_TEST_${Date.now()}`;
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: newFacultyId,
          name: 'Admin Created Faculty',
        }),
        { headers }
      );

      const success = check(facultyRes, {
        'admin can create faculty': (r) => r.status === 201 || r.status === 200,
      });
      
      if (!success) {
        console.error(`Admin create faculty failed: ${facultyRes.status} - ${facultyRes.body}`);
      }
    });

    sleep(1);

    // 2. List all faculties
    group('2. Admin lists all faculties', () => {
      const listRes = http.get(`${BASE_URL}/api/faculties`, { headers });
      check(listRes, {
        'admin can list faculties': (r) => r.status === 200,
        'faculties list not empty': (r) => Array.isArray(r.json()) && r.json().length > 0,
      });
    });

    sleep(1);

    // 3. General admin lists all rooms
    group('3. Admin lists all rooms', () => {
      const roomsRes = http.get(`${BASE_URL}/api/rooms`, { headers });
      check(roomsRes, {
        'admin can list all rooms': (r) => r.status === 200,
        'rooms list not empty': (r) => Array.isArray(r.json()) && r.json().length > 0,
      });
    });

    sleep(1);

    // 4. Create a room
    group('4. Admin creates room', () => {
      const roomId = `ADMIN_ROOM_${Date.now()}`;
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: roomId,
          facultyId: data.facultyId,
          capacity: 100,
          equipment: 'projector,whiteboard,computers,smartboard',
        }),
        { headers }
      );

      check(roomRes, {
        'admin can create room': (r) => r.status === 201 || r.status === 200,
      });
    });

    sleep(1);

    // 4. List all reservations
    group('4. Admin lists all reservations', () => {
      const reservationsRes = http.get(`${BASE_URL}/api/reservations`, { headers });
      check(reservationsRes, {
        'admin can list all reservations': (r) => r.status === 200,
      });
    });

    sleep(1);

    // 5. Create a test reservation to demonstrate status changes
    let testReservationId;
    group('5. Setup: Create test student and reservation', () => {
      const testStudent = `admin_test_student_${Date.now()}`;
      
      // Register student
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: testStudent,
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
          username: testStudent,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const studentToken = loginRes.json('token');

      sleep(0.5);

      // Create reservation
      const reservationTime = new Date(Date.now() + 120 * 60 * 60 * 1000); // 5 days ahead
      const endReservationTime = new Date(reservationTime.getTime() + 2 * 60 * 60 * 1000);

      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: reservationTime.toISOString().slice(0, 19),
          endDateTime: endReservationTime.toISOString().slice(0, 19),
          attendees: 20,
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

    // 6. Admin approves reservation
    group('6. Admin approves reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/approve`,
        null,
        { headers }
      );

      check(approveRes, {
        'admin can approve reservation': (r) => r.status === 200,
        'status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });
    });

    sleep(1);

    // 7. Admin revokes the approved reservation
    group('7. Admin revokes approved reservation', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/revoke`,
        null,
        { headers }
      );

      check(revokeRes, {
        'admin can revoke approved reservation': (r) => r.status === 200,
        'status changed to REVOKED': (r) => r.json('status') === 'REVOKED',
      });
    });

    sleep(1);

    // 8. Admin re-approves the revoked reservation
    group('8. Admin re-approves revoked reservation', () => {
      const reapproveRes = http.post(
        `${BASE_URL}/api/reservations/${testReservationId}/approve`,
        null,
        { headers }
      );

      check(reapproveRes, {
        'admin can re-approve revoked reservation': (r) => r.status === 200,
        'status changed back to APPROVED': (r) => r.json('status') === 'APPROVED',
      });
    });

    sleep(1);

    // 9. Update faculty policy
    if (data.policyId) {
      group('9. Admin updates policy', () => {
        const policyRes = http.put(
          `${BASE_URL}/api/policies/${data.policyId}`,
          JSON.stringify({
            facultyId: data.facultyId,
            maxReservationDurationHours: 6,
            advanceBookingDays: 60,
            allowWeekendBooking: false,
          }),
          { headers }
        );

        check(policyRes, {
          'admin can update policy': (r) => r.status === 200,
        });
      });
    }
  });

  workflowDuration.add(Date.now() - workflowStart);
  adminWorkflows.add(1);
  console.log('=== ADMIN WORKFLOW COMPLETE ===\n');
}
