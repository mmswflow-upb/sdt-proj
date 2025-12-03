import http from 'k6/http';
import { sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin';

export function setupTestEnvironment() {
  console.log('=== Setting up test environment ===');

  // Login as admin
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      username: ADMIN_USERNAME,
      password: ADMIN_PASSWORD,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  if (loginRes.status !== 200) {
    console.error(`Admin login failed!`);
    console.error(`  Status: ${loginRes.status}`);
    console.error(`  Response: ${loginRes.body}`);
    console.error(`  URL: ${BASE_URL}/api/auth/login`);
    console.error(`  Username: ${ADMIN_USERNAME}`);
    throw new Error(`Admin login failed: ${loginRes.status} - ${loginRes.body}`);
  }

  const adminToken = loginRes.json('token');

  // Create test faculty
  const facultyId = `TEST_FAC_${Date.now()}`;
  const facultyRes = http.post(
    `${BASE_URL}/api/faculties`,
    JSON.stringify({
      facultyId: facultyId,
      name: 'Test Faculty',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
    }
  );

  if (facultyRes.status !== 201 && facultyRes.status !== 200) {
    console.error(`Faculty creation failed!`);
    console.error(`  Status: ${facultyRes.status}`);
    console.error(`  Response: ${facultyRes.body}`);
    console.error(`  Token present: ${adminToken ? 'Yes' : 'No'}`);
    throw new Error(`Faculty creation failed: ${facultyRes.status} - ${facultyRes.body}`);
  }

  // Create test rooms
  const rooms = [];
  for (let i = 1; i <= 3; i++) {
    const roomId = `${facultyId}-ROOM-${String(i).padStart(2, '0')}`;
    const roomRes = http.post(
      `${BASE_URL}/api/rooms`,
      JSON.stringify({
        roomId: roomId,
        facultyId: facultyId,
        capacity: 30 + i * 10,
        equipment: 'projector,whiteboard,computers',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${adminToken}`,
        },
      }
    );
    
    if (roomRes.status === 201 || roomRes.status === 200) {
      rooms.push(roomId);
      console.log(`  ✓ Created room: ${roomId}`);
    }
    sleep(0.3);
  }

  // Create a faculty policy
  const policyRes = http.post(
    `${BASE_URL}/api/policies`,
    JSON.stringify({
      facultyId: facultyId,
      maxReservationDurationHours: 4,
      advanceBookingDays: 30,
      allowWeekendBooking: true,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`,
      },
    }
  );

  let policyId = null;
  if (policyRes.status === 201 || policyRes.status === 200) {
    policyId = policyRes.json('id');
    console.log(`  ✓ Created policy: ${policyId}`);
  }

  console.log(`=== Setup complete: Faculty ${facultyId} with ${rooms.length} rooms ===\n`);

  return { adminToken, facultyId, rooms, policyId };
}

export function teardownTestEnvironment(data) {
  console.log('\n=== TEST SUMMARY ===');
  console.log(`Faculty ID: ${data.facultyId}`);
  console.log(`Rooms Created: ${data.rooms.length}`);
  console.log(`Policy ID: ${data.policyId || 'N/A'}`);
  console.log('Check metrics above for workflow results, conflicts, and authorization checks');
  console.log('===================\n');
}
