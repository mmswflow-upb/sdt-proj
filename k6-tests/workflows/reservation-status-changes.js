import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const statusChangeWorkflows = new Counter('status_change_workflows');
const workflowDuration = new Trend('status_change_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Reservation Status Changes Workflow
 * Tests revoke and re-approve operations
 */
export function reservationStatusChangesWorkflow(data, users, reservations, approvals) {
  console.log('\n=== RESERVATION STATUS CHANGES WORKFLOW START ===');
  const workflowStart = Date.now();

  const statusChanges = {
    revokedId: null,
    reApprovedId: null,
  };

  group('Reservation Status Changes Workflow', () => {
    const adminHeaders = createAuthHeaders(users.admin.token);
    const facultyAdminHeaders = createAuthHeaders(users.facultyAdmin.token);
    const studentHeaders = createAuthHeaders(users.student.token);

    // 1. Admin revokes an approved reservation
    group('1. Admin revokes approved reservation', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${approvals.adminApprovedId}/revoke`,
        null,
        { headers: adminHeaders }
      );

      const success = check(revokeRes, {
        'admin can revoke reservation': (r) => r.status === 200,
        'reservation status is REVOKED': (r) => r.json('status') === 'REVOKED',
      });

      if (success) {
        statusChanges.revokedId = approvals.adminApprovedId;
        console.log(`  ✓ Admin revoked reservation: ${approvals.adminApprovedId}`);
      } else {
        console.error(`Admin revoke failed: ${revokeRes.status} - ${revokeRes.body}`);
      }
    });

    sleep(1);

    // 2. Student views revoked reservation
    group('2. Student views revoked reservation', () => {
      const revokedRes = http.get(
        `${BASE_URL}/api/reservations/me?status=REVOKED`,
        { headers: studentHeaders }
      );

      check(revokedRes, {
        'student can filter reservations by REVOKED status': (r) => r.status === 200,
        'student has at least one revoked reservation': (r) => 
          Array.isArray(r.json()) && r.json().some(res => res.id === statusChanges.revokedId),
      });

      const count = Array.isArray(revokedRes.json()) ? revokedRes.json().length : 0;
      console.log(`  ✓ Student sees ${count} revoked reservations`);
    });

    sleep(1);

    // 3. Faculty admin re-approves the revoked reservation
    group('3. Faculty admin re-approves revoked reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${statusChanges.revokedId}/approve`,
        null,
        { headers: facultyAdminHeaders }
      );

      const success = check(approveRes, {
        'faculty admin can re-approve': (r) => r.status === 200,
        'status changed back to APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      if (success) {
        statusChanges.reApprovedId = statusChanges.revokedId;
        console.log(`  ✓ Faculty admin re-approved reservation: ${statusChanges.revokedId}`);
      } else {
        console.error(`Faculty admin re-approve failed: ${approveRes.status} - ${approveRes.body}`);
      }
    });

    sleep(1);

    // 4. Admin revokes again (testing cycle)
    group('4. Admin revokes again', () => {
      const revokeRes = http.post(
        `${BASE_URL}/api/reservations/${statusChanges.reApprovedId}/revoke`,
        null,
        { headers: adminHeaders }
      );

      check(revokeRes, {
        'can revoke re-approved reservation': (r) => r.status === 200,
        'status is REVOKED again': (r) => r.json('status') === 'REVOKED',
      });

      console.log(`  ✓ Admin revoked reservation again`);
    });

    sleep(1);

    // 5. Admin re-approves (complete the cycle)
    group('5. Admin re-approves reservation', () => {
      const approveRes = http.post(
        `${BASE_URL}/api/reservations/${statusChanges.reApprovedId}/approve`,
        null,
        { headers: adminHeaders }
      );

      check(approveRes, {
        'admin can re-approve revoked': (r) => r.status === 200,
        'final status is APPROVED': (r) => r.json('status') === 'APPROVED',
      });

      console.log(`  ✓ Admin re-approved reservation, completing status cycle`);
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  statusChangeWorkflows.add(1);
  console.log('=== RESERVATION STATUS CHANGES WORKFLOW COMPLETE ===\n');

  return statusChanges;
}
