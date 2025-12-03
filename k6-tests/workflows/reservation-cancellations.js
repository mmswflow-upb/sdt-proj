import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders, registerAndLogin } from './auth-helpers.js';

const cancellationWorkflows = new Counter('cancellation_workflows');
const workflowDuration = new Trend('cancellation_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Reservation Cancellations Workflow
 * Tests cancel functionality and related edge cases
 */
export function reservationCancellationsWorkflow(data, users) {
  console.log('\n=== RESERVATION CANCELLATIONS WORKFLOW START ===');
  const workflowStart = Date.now();

  const cancellations = {
    cancelledReservationId: null,
    rebookedReservationId: null,
  };

  group('Reservation Cancellations Workflow', () => {
    const studentHeaders = createAuthHeaders(users.student.token);
    const adminHeaders = createAuthHeaders(users.admin.token);

    // 1. Student creates reservation for cancel test
    let cancelTestReservationId;
    group('1. Student creates reservation for cancel test', () => {
      const cancelTestTime = new Date(Date.now() + 144 * 60 * 60 * 1000); // 6 days ahead
      const cancelTestEndTime = new Date(cancelTestTime.getTime() + 1 * 60 * 60 * 1000);

      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: cancelTestTime.toISOString().slice(0, 19),
          endDateTime: cancelTestEndTime.toISOString().slice(0, 19),
          attendees: 15,
        }),
        { headers: studentHeaders }
      );

      const success = check(reservationRes, {
        'student can create reservation for cancel test': (r) => r.status === 201 || r.status === 200,
        'cancel test reservation has ID': (r) => r.json('id') !== undefined,
      });

      if (success) {
        cancelTestReservationId = reservationRes.json('id');
        console.log(`  ✓ Created reservation for cancel test: ${cancelTestReservationId}`);
      }
    });

    sleep(1);

    // 2. Admin approves the cancel test reservation
    group('2. Admin approves cancel test reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${cancelTestReservationId}/approve`,
        null,
        { headers: adminHeaders }
      );

      check(approveRes, {
        'admin approves cancel test reservation successfully': (r) => r.status === 200,
        'cancel test reservation status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      console.log(`  ✓ Admin approved cancel test reservation`);
    });

    sleep(1);

    // 3. Student cancels their own approved reservation
    group('3. Student cancels approved reservation', () => {
      const cancelRes = http.post(
        `${BASE_URL}/api/reservations/${cancelTestReservationId}/cancel`,
        null,
        { headers: studentHeaders }
      );

      const success = check(cancelRes, {
        'student can cancel their own approved reservation': (r) => r.status === 200,
        'cancelled reservation status is CANCELLED': (r) => r.json('status') === 'CANCELLED',
      });

      if (success) {
        cancellations.cancelledReservationId = cancelTestReservationId;
        console.log(`  ✓ Student cancelled reservation: ${cancelTestReservationId}`);
      } else {
        console.error(`Cancel failed: ${cancelRes.status} - ${cancelRes.body}`);
      }
    });

    sleep(1);

    // 4. Verify cancelled reservation cannot be approved by admin (immutability test)
    group('4. Verify cancelled cannot be approved', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${cancellations.cancelledReservationId}/approve`,
        null,
        { headers: adminHeaders }
      );

      const errorMessage = approveRes.body || '';
      const success = check(approveRes, {
        'admin cannot approve a cancelled reservation (returns 400)': (r) => r.status === 400 || r.status === 409,
        'error message indicates cancelled reservations are immutable': (r) => {
          const body = r.body ? r.body.toLowerCase() : '';
          return body.includes('cancel') || body.includes('cannot') || body.includes('immutable');
        },
      });

      if (!success) {
        console.error(`  ✗ Immutability check failed. Status: ${approveRes.status}, Body: ${errorMessage}`);
      } else {
        console.log(`  ✓ Verified cancelled reservation is immutable (error: "${errorMessage}")`);
      }
    });

    sleep(1);

    // 5. Student re-books same slot after cancellation
    group('5. Student re-books same slot', () => {
      const cancelTestTime = new Date(Date.now() + 144 * 60 * 60 * 1000);
      const cancelTestEndTime = new Date(cancelTestTime.getTime() + 1 * 60 * 60 * 1000);

      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: cancelTestTime.toISOString().slice(0, 19),
          endDateTime: cancelTestEndTime.toISOString().slice(0, 19),
          attendees: 15,
        }),
        { headers: studentHeaders }
      );

      const success = check(reservationRes, {
        'student can rebook the same slot after cancellation': (r) => r.status === 201 || r.status === 200,
        'rebooked reservation has a new different ID': (r) => 
          r.json('id') !== undefined && r.json('id') !== cancellations.cancelledReservationId,
      });

      if (success) {
        cancellations.rebookedReservationId = reservationRes.json('id');
        console.log(`  ✓ Successfully rebooked same slot with new ID: ${cancellations.rebookedReservationId}`);
      } else {
        console.error(`  ✗ Rebook failed. Status: ${reservationRes.status}, Body: ${reservationRes.body}`);
        console.error(`    Cancelled ID: ${cancellations.cancelledReservationId}, Room: ${data.rooms[0]}`);
        console.error(`    Time: ${cancelTestTime.toISOString().slice(0, 19)} to ${cancelTestEndTime.toISOString().slice(0, 19)}`);
      }
    });

    sleep(1);

    // 6. Student cannot cancel another student's reservation
    group('6. Student cannot cancel other student reservation', () => {
      // Use second student to create a reservation
      const student2Headers = createAuthHeaders(users.student2.token);

      // Student2 creates a reservation
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
        { headers: student2Headers }
      );

      const otherReservationId = otherReservationRes.json('id');

      sleep(0.5);

      // Original student tries to cancel other student's reservation
      const cancelAttemptRes = http.post(
        `${BASE_URL}/api/reservations/${otherReservationId}/cancel`,
        null,
        { headers: studentHeaders }
      );

      check(cancelAttemptRes, {
        'student cannot cancel another student\'s reservation (403/404)': (r) => r.status === 403 || r.status === 404,
      });

      console.log(`  ✓ Verified student cannot cancel other student's reservation`);
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  cancellationWorkflows.add(1);
  console.log('=== RESERVATION CANCELLATIONS WORKFLOW COMPLETE ===\n');

  return cancellations;
}
