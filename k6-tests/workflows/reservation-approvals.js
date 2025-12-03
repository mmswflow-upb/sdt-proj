import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const approvalWorkflows = new Counter('reservation_approval_workflows');
const workflowDuration = new Trend('reservation_approval_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Reservation Approvals Workflow
 * Tests approval operations by admin and faculty admin
 */
export function reservationApprovalsWorkflow(data, users, reservations) {
  console.log('\n=== RESERVATION APPROVALS WORKFLOW START ===');
  const workflowStart = Date.now();

  const approvals = {
    adminApprovedId: null,
    facultyAdminApprovedId: null,
  };

  group('Reservation Approvals Workflow', () => {
    const adminHeaders = createAuthHeaders(users.admin.token);
    const facultyAdminHeaders = createAuthHeaders(users.facultyAdmin.token);
    const studentHeaders = createAuthHeaders(users.student.token);

    // 1. Admin approves reservation
    group('1. Admin approves student reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${reservations.mainReservationId}/approve`,
        null,
        { headers: adminHeaders }
      );

      const success = check(approveRes, {
        'admin can approve reservation': (r) => r.status === 200,
        'reservation status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      if (success) {
        approvals.adminApprovedId = reservations.mainReservationId;
        console.log(`  ✓ Admin approved reservation: ${reservations.mainReservationId}`);
      } else {
        console.error(`Admin approval failed: ${approveRes.status} - ${approveRes.body}`);
      }
    });

    sleep(1);

    // 2. Student verifies approval
    group('2. Student verifies approval', () => {
      const reservationRes = http.get(
        `${BASE_URL}/api/reservations/${reservations.mainReservationId}`,
        { headers: studentHeaders }
      );

      check(reservationRes, {
        'can retrieve reservation': (r) => r.status === 200,
        'reservation is approved': (r) => r.json('status') === 'APPROVED',
      });

      console.log(`  ✓ Student verified approval`);
    });

    sleep(1);

    // 3. Faculty admin approves a concurrent reservation
    group('3. Faculty admin approves reservation', () => {
      if (reservations.concurrentReservationIds.length > 0) {
        const reservationId = reservations.concurrentReservationIds[0];
        const approveRes = http.post(
          `${BASE_URL}/api/reservations/${reservationId}/approve`,
          null,
          { headers: facultyAdminHeaders }
        );

        const success = check(approveRes, {
          'faculty admin can approve reservation': (r) => r.status === 200,
          'status changed to APPROVED': (r) => r.json('status') === 'APPROVED',
        });

        if (success) {
          approvals.facultyAdminApprovedId = reservationId;
          console.log(`  ✓ Faculty admin approved reservation: ${reservationId}`);
        } else {
          console.error(`Faculty admin approval failed: ${approveRes.status} - ${approveRes.body}`);
        }
      } else {
        console.warn('  ⚠ No concurrent reservations to approve');
      }
    });

    sleep(1);

    // 4. Admin lists all reservations
    group('4. Admin lists all reservations', () => {
      const reservationsRes = http.get(
        `${BASE_URL}/api/reservations`,
        { headers: adminHeaders }
      );

      check(reservationsRes, {
        'admin can list all reservations in the system': (r) => r.status === 200,
        'admin reservations response is an array': (r) => Array.isArray(r.json()),
      });

      const count = Array.isArray(reservationsRes.json()) ? reservationsRes.json().length : 0;
      console.log(`  ✓ Admin sees ${count} total reservations`);
    });

    sleep(1);

    // 5. Faculty admin lists reservations
    group('5. Faculty admin lists reservations', () => {
      const reservationsRes = http.get(
        `${BASE_URL}/api/reservations`,
        { headers: facultyAdminHeaders }
      );

      check(reservationsRes, {
        'faculty admin can list reservations': (r) => r.status === 200,
        'faculty admin reservations response is an array': (r) => Array.isArray(r.json()),
      });

      const count = Array.isArray(reservationsRes.json()) ? reservationsRes.json().length : 0;
      console.log(`  ✓ Faculty admin sees ${count} reservations`);
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  approvalWorkflows.add(1);
  console.log('=== RESERVATION APPROVALS WORKFLOW COMPLETE ===\n');

  return approvals;
}
