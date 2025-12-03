import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const conflictDetected = new Counter('conflict_detected');
const duplicateRejected = new Counter('duplicate_rejected');
const unauthorizedBlocked = new Counter('unauthorized_blocked');
const workflowDuration = new Trend('workflow_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'conflict_detected': ['count>0'],
    'duplicate_rejected': ['count>0'],
    'unauthorized_blocked': ['count>0'],
  },
};

export function edgeCasesWorkflow(data) {
  console.log('\n=== EDGE CASES WORKFLOW START ===');
  const workflowStart = Date.now();

  const roomId = data.rooms[0];
  const baseTime = new Date(Date.now() + 72 * 60 * 60 * 1000); // 3 days ahead
  const baseTimeMs = baseTime.getTime();
  const startTime = baseTime.toISOString().slice(0, 19);
  const endTimeMs = baseTimeMs + 2 * 60 * 60 * 1000;
  const endTime = new Date(endTimeMs).toISOString().slice(0, 19);

  group('Edge Cases Workflow', () => {
    // Setup: Create first student and reservation
    const student1 = `edge_student1_${__VU}_${Date.now()}`;
    let student1Token, reservationId;

    group('Setup: Create first student and reservation', () => {
      // Register student 1
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: student1,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login student 1
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: student1,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      student1Token = loginRes.json('token');

      sleep(0.5);

      // Create first reservation
      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: startTime,
          endDateTime: endTime,
          attendees: 10,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${student1Token}`,
          },
        }
      );

      check(reservationRes, {
        'first reservation created': (r) => r.status === 201 || r.status === 200,
      });

      reservationId = reservationRes.json('id');
      console.log(`  ✓ Created reservation ${reservationId} for ${roomId} at ${startTime}`);
    });

    sleep(2);

    // EDGE CASE 1: Try to create duplicate reservation (same user, same time/room)
    group('Edge Case 1: Duplicate reservation attempt', () => {
      const duplicateRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: startTime,
          endDateTime: endTime,
          attendees: 10,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${student1Token}`,
          },
        }
      );

      const isDuplicate = check(duplicateRes, {
        'duplicate reservation rejected': (r) => r.status === 409 || r.status === 400 || r.status === 500,
      });

      if (isDuplicate) {
        duplicateRejected.add(1);
        console.log('  ✓ Duplicate reservation correctly rejected');
      } else {
        console.error(`  ✗ Duplicate reservation not rejected: ${duplicateRes.status}`);
        console.error(`  Response: ${duplicateRes.body}`);
      }
    });

    sleep(2);

    // EDGE CASE 2: Different user tries to book same room at same time
    group('Edge Case 2: Conflicting reservation (different user)', () => {
      const student2 = `edge_student2_${__VU}_${Date.now()}`;

      // Register student 2
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: student2,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login student 2
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: student2,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const student2Token = loginRes.json('token');

      sleep(0.5);

      // Try to book the same slot
      const conflictRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: startTime,
          endDateTime: endTime,
          attendees: 10,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${student2Token}`,
          },
        }
      );

      const hasConflict = check(conflictRes, {
        'conflicting reservation rejected': (r) => r.status === 409 || r.status === 400 || r.status === 500,
      });

      if (hasConflict) {
        conflictDetected.add(1);
        console.log('  ✓ Conflicting reservation correctly rejected');
      } else {
        console.error(`  ✗ Conflicting reservation not rejected: ${conflictRes.status}`);
        console.error(`  Response: ${conflictRes.body}`);
      }
    });

    sleep(2);

    // EDGE CASE 3: Overlapping time slot (partial overlap)
    group('Edge Case 3: Overlapping time slot', () => {
      const student3 = `edge_student3_${__VU}_${Date.now()}`;

      // Register student 3
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: student3,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login student 3
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: student3,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const student3Token = loginRes.json('token');

      sleep(0.5);

      // Try to book overlapping slot (starts 1 hour into existing reservation)
      const overlapStartTime = new Date(baseTime.getTime() + 60 * 60 * 1000).toISOString().slice(0, 19);
      const overlapEndTime = new Date(baseTime.getTime() + 4 * 60 * 60 * 1000).toISOString().slice(0, 19);

      const overlapRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: overlapStartTime,
          endDateTime: overlapEndTime,
          attendees: 10,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${student3Token}`,
          },
        }
      );

      const hasOverlap = check(overlapRes, {
        'overlapping reservation rejected': (r) => r.status === 409 || r.status === 400 || r.status === 500,
      });

      if (hasOverlap) {
        conflictDetected.add(1);
        console.log('  ✓ Overlapping reservation correctly rejected');
      } else {
        console.error(`  ✗ Overlapping reservation not rejected: ${overlapRes.status}`);
        console.error(`  Response: ${overlapRes.body}`);
      }
    });

    sleep(2);

    // EDGE CASE 4: Back-to-back reservations (should succeed)
    group('Edge Case 4: Back-to-back reservation (should succeed)', () => {
      const student4 = `edge_student4_${__VU}_${Date.now()}`;

      // Register student 4
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: student4,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login student 4
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: student4,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const student4Token = loginRes.json('token');

      sleep(0.5);

      // Book immediately after the first reservation ends
      const nextStartTime = endTime;
      const nextEndTimeMs = endTimeMs + 2 * 60 * 60 * 1000;
      const nextEndTime = new Date(nextEndTimeMs).toISOString().slice(0, 19);

      console.log(`  Back-to-back times: start=${nextStartTime}, end=${nextEndTime}`);

      const backToBackRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: roomId,
          startDateTime: nextStartTime,
          endDateTime: nextEndTime,
          attendees: 10,
        }),
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${student4Token}`,
          },
        }
      );

      const success = check(backToBackRes, {
        'back-to-back reservation succeeds': (r) => r.status === 201 || r.status === 200,
      });

      if (success) {
        console.log('  ✓ Back-to-back reservation allowed');
      } else {
        console.error(`  ✗ Back-to-back reservation failed: ${backToBackRes.status}`);
        console.error(`  Response: ${backToBackRes.body}`);
      }
    });

    sleep(2);

    // EDGE CASE 5: Student tries to revoke someone else's reservation
    group('Edge Case 5: Unauthorized revoke attempt', () => {
      const student5 = `edge_student5_${__VU}_${Date.now()}`;

      // Register student 5
      http.post(
        `${BASE_URL}/api/auth/register`,
        JSON.stringify({
          username: student5,
          password: 'password123',
          role: 'STUDENT',
          facultyId: data.facultyId,
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      sleep(0.5);

      // Login student 5
      const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
          username: student5,
          password: 'password123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const student5Token = loginRes.json('token');

      sleep(0.5);

      // Try to revoke student1's reservation
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${reservationId}/revoke`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${student5Token}`,
          },
        }
      );

      const isUnauthorized = check(revokeRes, {
        'unauthorized revoke rejected': (r) => r.status === 403 || r.status === 401,
      });

      if (isUnauthorized) {
        unauthorizedBlocked.add(1);
        console.log('  ✓ Unauthorized revoke correctly rejected');
      }
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  console.log('=== EDGE CASES WORKFLOW COMPLETE ===\n');
}
