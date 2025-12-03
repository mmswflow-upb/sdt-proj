import { setupTestEnvironment, teardownTestEnvironment } from './workflows/common-setup.js';
import { authSetupWorkflow } from './workflows/auth-setup.js';
import { resourceManagementWorkflow } from './workflows/resource-management.js';
import { studentReservationsWorkflow } from './workflows/student-reservations.js';
import { reservationApprovalsWorkflow } from './workflows/reservation-approvals.js';
import { reservationStatusChangesWorkflow } from './workflows/reservation-status-changes.js';
import { reservationCancellationsWorkflow } from './workflows/reservation-cancellations.js';
import { authorizationTestsWorkflow } from './workflows/authorization-tests.js';
import { roomDeletionWorkflow } from './workflows/room-deletion.js';

export const options = {
  scenarios: {
    sequential_workflows: {
      executor: 'per-vu-iterations',
      vus: 1,               // a single VU
      iterations: 1,        // it will run runAllWorkflows once
      maxDuration: '20m',   // just a safe upper bound
      exec: 'runAllWorkflows',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.1'],
    'auth_workflows': ['count>0'],
    'resource_management_workflows': ['count>0'],
    'student_reservation_workflows': ['count>0'],
    'reservation_approval_workflows': ['count>0'],
    'status_change_workflows': ['count>0'],
    'cancellation_workflows': ['count>0'],
    'authorization_test_workflows': ['count>0'],
    'room_deletion_workflows': ['count>0'],
    'unauthorized_operations_blocked': ['count>0'],
  },
};

export function setup() {
  return setupTestEnvironment();
}

export function runAllWorkflows(data) {
  // 1. Setup authentication - create users for all tests
  const users = authSetupWorkflow(data);

  // 2. Resource management - create faculties and rooms
  const resources = resourceManagementWorkflow(data, users);

  // 3. Student reservations - create and view reservations
  const reservations = studentReservationsWorkflow(data, users);

  // 4. Reservation approvals - admin and faculty admin approve
  const approvals = reservationApprovalsWorkflow(data, users, reservations);

  // 5. Status changes - revoke and re-approve cycle
  const statusChanges = reservationStatusChangesWorkflow(data, users, reservations, approvals);

  // 6. Cancellations - cancel, verify immutability, rebook
  const cancellations = reservationCancellationsWorkflow(data, users);

  // 7. Room deletion - test cascade effects on reservations and schedules
  const deletionData = roomDeletionWorkflow(data, users);

  // 8. Authorization tests - verify access control (run last)
  authorizationTestsWorkflow(data, users, reservations);
}

export function teardown(data) {
  teardownTestEnvironment(data);
}
