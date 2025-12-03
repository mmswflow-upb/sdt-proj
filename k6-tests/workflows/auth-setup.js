import { group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { registerAndLogin } from './auth-helpers.js';

const authWorkflows = new Counter('auth_workflows');
const workflowDuration = new Trend('auth_workflow_duration');

/**
 * Authentication Setup Workflow
 * Creates users for all test scenarios
 */
export function authSetupWorkflow(data) {
  console.log('\n=== AUTH SETUP WORKFLOW START ===');
  const workflowStart = Date.now();

  const users = {};

  group('Auth Setup Workflow', () => {
    // 1. Create student user
    group('1. Register and login student', () => {
      const username = `student_${Date.now()}`;
      const result = registerAndLogin(
        username,
        'student123',
        'STUDENT',
        data.facultyId,
        { email: `${username}@university.com`, firstName: 'Test', lastName: 'Student' }
      );
      users.student = result;
      console.log(`  ✓ Student created: ${username}`);
    });

    sleep(1);

    // 2. Create faculty admin user
    group('2. Register and login faculty admin', () => {
      const username = `faculty_admin_${Date.now()}`;
      const result = registerAndLogin(
        username,
        'admin123',
        'FACULTY_ADMIN',
        data.facultyId,
        { email: `${username}@university.com`, firstName: 'Test', lastName: 'FacultyAdmin' }
      );
      users.facultyAdmin = result;
      console.log(`  ✓ Faculty admin created: ${username}`);
    });

    sleep(1);

    // 3. Create additional student for cross-user tests
    group('3. Register and login second student', () => {
      const username = `student2_${Date.now()}`;
      const result = registerAndLogin(
        username,
        'student123',
        'STUDENT',
        data.facultyId,
        { email: `${username}@university.com`, firstName: 'Test', lastName: 'Student2' }
      );
      users.student2 = result;
      console.log(`  ✓ Second student created: ${username}`);
    });

    sleep(1);

    // Store admin token from setup
    users.admin = {
      token: data.adminToken,
      username: 'admin',
      role: 'ADMIN'
    };
  });

  workflowDuration.add(Date.now() - workflowStart);
  authWorkflows.add(1);
  console.log('=== AUTH SETUP WORKFLOW COMPLETE ===\n');

  // Return users object to be used by other workflows
  return users;
}
