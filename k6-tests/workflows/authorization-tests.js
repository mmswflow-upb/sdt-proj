import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const authorizationWorkflows = new Counter('authorization_test_workflows');
const unauthorizedBlocked = new Counter('unauthorized_operations_blocked');
const workflowDuration = new Trend('authorization_test_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Authorization Tests Workflow
 * Tests all negative authorization cases - operations that should fail
 */
export function authorizationTestsWorkflow(data, users, reservations) {
  console.log('\n=== AUTHORIZATION TESTS WORKFLOW START ===');
  const workflowStart = Date.now();

  group('Authorization Tests Workflow', () => {
    const studentHeaders = createAuthHeaders(users.student.token);
    const facultyAdminHeaders = createAuthHeaders(users.facultyAdmin.token);

    // ===== Student Authorization Tests =====

    // 1. Student cannot approve reservations
    group('1. Student cannot approve reservations', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${reservations.mainReservationId}/approve`,
        null,
        { headers: studentHeaders }
      );

      const success = check(approveRes, {
        'student cannot approve reservations (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from approving`);
      } else {
        console.error(`Authorization check failed: ${approveRes.status}`);
      }
    });

    sleep(1);

    // 2. Student cannot revoke reservations
    group('2. Student cannot revoke reservations', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${reservations.mainReservationId}/revoke`,
        null,
        { headers: studentHeaders }
      );

      const success = check(revokeRes, {
        'student cannot revoke reservations (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from revoking`);
      } else {
        console.error(`Authorization check failed: ${revokeRes.status}`);
      }
    });

    sleep(1);

    // 3. Student cannot create rooms
    group('3. Student cannot create rooms', () => {
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: `STUDENT_ROOM_${Date.now()}`,
          facultyId: data.facultyId,
          capacity: 30,
          equipment: 'projector',
        }),
        { headers: studentHeaders }
      );

      const success = check(roomRes, {
        'student cannot create rooms (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from creating room`);
      } else {
        console.error(`Authorization check failed: ${roomRes.status}`);
      }
    });

    sleep(1);

    // 4. Student cannot delete rooms
    group('4. Student cannot delete rooms', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/rooms/${data.rooms[0]}`,
        null,
        { headers: studentHeaders }
      );

      const success = check(deleteRes, {
        'student cannot delete rooms (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from deleting room`);
      } else {
        console.error(`Authorization check failed: ${deleteRes.status}`);
      }
    });

    sleep(1);

    // 5. Student cannot create faculties
    group('5. Student cannot create faculties', () => {
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: `STUDENT_FAC_${Date.now()}`,
          name: 'Student Created Faculty',
        }),
        { headers: studentHeaders }
      );

      const success = check(facultyRes, {
        'student cannot create faculties (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from creating faculty`);
      } else {
        console.error(`Authorization check failed: ${facultyRes.status}`);
      }
    });

    sleep(1);

    // 6. Student cannot delete faculties
    group('6. Student cannot delete faculties', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/faculties/${data.facultyId}`,
        null,
        { headers: studentHeaders }
      );

      const success = check(deleteRes, {
        'student cannot delete faculties (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Student correctly blocked from deleting faculty`);
      } else {
        console.error(`Authorization check failed: ${deleteRes.status}`);
      }
    });

    sleep(1);

    // ===== Faculty Admin Authorization Tests =====

    // 7. Faculty admin cannot create faculties
    group('7. Faculty admin cannot create faculties', () => {
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: `FADMIN_FAC_${Date.now()}`,
          name: 'Faculty Admin Created Faculty',
        }),
        { headers: facultyAdminHeaders }
      );

      const success = check(facultyRes, {
        'faculty admin cannot create faculties (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Faculty admin correctly blocked from creating faculty`);
      } else {
        console.error(`Authorization check failed: ${facultyRes.status}`);
      }
    });

    sleep(1);

    // 8. Faculty admin cannot delete faculties
    group('8. Faculty admin cannot delete faculties', () => {
      const deleteRes = http.del(
        `${BASE_URL}/api/faculties/${data.facultyId}`,
        null,
        { headers: facultyAdminHeaders }
      );

      const success = check(deleteRes, {
        'faculty admin cannot delete faculties (403/401)': (r) => r.status === 403 || r.status === 401,
      });

      if (success) {
        unauthorizedBlocked.add(1);
        console.log(`  ✓ Faculty admin correctly blocked from deleting faculty`);
      } else {
        console.error(`Authorization check failed: ${deleteRes.status}`);
      }
    });

    console.log(`\n  ✓ Total unauthorized operations blocked: ${unauthorizedBlocked}`);
  });

  workflowDuration.add(Date.now() - workflowStart);
  authorizationWorkflows.add(1);
  console.log('=== AUTHORIZATION TESTS WORKFLOW COMPLETE ===\n');
}
