import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const resourceWorkflows = new Counter('resource_management_workflows');
const workflowDuration = new Trend('resource_management_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Resource Management Workflow
 * Tests CRUD operations for faculties and rooms
 */
export function resourceManagementWorkflow(data, users) {
  console.log('\n=== RESOURCE MANAGEMENT WORKFLOW START ===');
  const workflowStart = Date.now();

  const resources = {
    createdFacultyId: null,
    createdRoomId: null,
  };

  group('Resource Management Workflow', () => {
    const adminHeaders = createAuthHeaders(users.admin.token);
    const facultyAdminHeaders = createAuthHeaders(users.facultyAdmin.token);

    // 1. Admin creates a new faculty
    group('1. Admin creates faculty', () => {
      const newFacultyId = `ADMIN_TEST_${Date.now()}`;
      const facultyRes = http.post(
        `${BASE_URL}/api/faculties`,
        JSON.stringify({
          facultyId: newFacultyId,
          name: 'Admin Created Faculty',
        }),
        { headers: adminHeaders }
      );

      const success = check(facultyRes, {
        'admin can create faculty': (r) => r.status === 201 || r.status === 200,
      });
      
      if (success) {
        resources.createdFacultyId = newFacultyId;
        console.log(`  ✓ Faculty created: ${newFacultyId}`);
      } else {
        console.error(`Admin create faculty failed: ${facultyRes.status} - ${facultyRes.body}`);
      }
    });

    sleep(1);

    // 2. Admin creates a room
    group('2. Admin creates room', () => {
      const roomId = `${data.facultyId}-ADMIN-ROOM-${Date.now()}`;
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: roomId,
          facultyId: data.facultyId,
          capacity: 50,
          equipment: 'projector,whiteboard',
        }),
        { headers: adminHeaders }
      );

      check(roomRes, {
        'admin can create room': (r) => r.status === 201 || r.status === 200,
      });

      console.log(`  ✓ Admin created room: ${roomId}`);
    });

    sleep(1);

    // 3. Admin lists rooms
    group('3. Admin lists all rooms', () => {
      const roomsRes = http.get(
        `${BASE_URL}/api/rooms`,
        { headers: adminHeaders }
      );

      check(roomsRes, {
        'admin can list rooms': (r) => r.status === 200,
        'rooms list is array': (r) => Array.isArray(r.json()),
      });

      const roomCount = Array.isArray(roomsRes.json()) ? roomsRes.json().length : 0;
      console.log(`  ✓ Admin retrieved ${roomCount} rooms`);
    });

    sleep(1);

    // 4. Faculty admin creates a room in their faculty
    group('4. Faculty admin creates room', () => {
      const roomId = `${data.facultyId}-FADMIN-${Date.now()}`;
      const roomRes = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
          roomId: roomId,
          facultyId: data.facultyId,
          capacity: 40,
          equipment: 'projector,computers',
        }),
        { headers: facultyAdminHeaders }
      );

      const success = check(roomRes, {
        'faculty admin can create room': (r) => r.status === 201 || r.status === 200,
      });

      if (success) {
        resources.createdRoomId = roomId;
        console.log(`  ✓ Faculty admin created room: ${roomId}`);
      } else {
        console.error(`Faculty admin create room failed: ${roomRes.status} - ${roomRes.body}`);
      }
    });

    sleep(1);

    // 5. Faculty admin lists rooms (should see their faculty's rooms)
    group('5. Faculty admin lists rooms', () => {
      const roomsRes = http.get(
        `${BASE_URL}/api/rooms`,
        { headers: facultyAdminHeaders }
      );

      check(roomsRes, {
        'faculty admin can list rooms': (r) => r.status === 200,
        'faculty rooms list is array': (r) => Array.isArray(r.json()),
      });

      const roomCount = Array.isArray(roomsRes.json()) ? roomsRes.json().length : 0;
      console.log(`  ✓ Faculty admin retrieved ${roomCount} rooms`);
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  resourceWorkflows.add(1);
  console.log('=== RESOURCE MANAGEMENT WORKFLOW COMPLETE ===\n');

  return resources;
}
