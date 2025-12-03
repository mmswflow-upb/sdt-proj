import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { createAuthHeaders } from './auth-helpers.js';

const studentReservationWorkflows = new Counter('student_reservation_workflows');
const workflowDuration = new Trend('student_reservation_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Student Reservations Workflow
 * Tests core student reservation operations
 */
export function studentReservationsWorkflow(data, users) {
  console.log('\n=== STUDENT RESERVATIONS WORKFLOW START ===');
  const workflowStart = Date.now();

  const reservations = {
    mainReservationId: null,
    concurrentReservationIds: [],
  };

  group('Student Reservations Workflow', () => {
    const studentHeaders = createAuthHeaders(users.student.token);

    // 1. Check room availability
    group('1. Student checks room availability', () => {
      const startTime = new Date(Date.now() + 24 * 60 * 60 * 1000); // Tomorrow
      const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000); // +2 hours

      const availabilityRes = http.get(
        `${BASE_URL}/api/availability?roomId=${data.rooms[0]}&from=${startTime.toISOString().slice(0, 19)}&to=${endTime.toISOString().slice(0, 19)}`,
        { headers: studentHeaders }
      );

      check(availabilityRes, {
        'student can check room availability': (r) => r.status === 200,
      });

      console.log(`  ✓ Availability checked for room ${data.rooms[0]}`);
    });

    sleep(1);

    // 2. Create reservation
    group('2. Student creates reservation', () => {
      const startTime = new Date(Date.now() + 48 * 60 * 60 * 1000); // 2 days ahead
      const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000);

      const reservationRes = http.post(
        `${BASE_URL}/api/reservations`,
        JSON.stringify({
          roomId: data.rooms[0],
          startDateTime: startTime.toISOString().slice(0, 19),
          endDateTime: endTime.toISOString().slice(0, 19),
          attendees: 25,
        }),
        { headers: studentHeaders }
      );

      const success = check(reservationRes, {
        'student can create reservation': (r) => r.status === 201 || r.status === 200,
        'reservation has ID': (r) => r.json('id') !== undefined,
        'reservation status is PENDING': (r) => r.json('status') === 'PENDING',
      });

      if (success) {
        reservations.mainReservationId = reservationRes.json('id');
        console.log(`  ✓ Reservation created: ${reservations.mainReservationId}`);
      } else {
        console.error(`Reservation creation failed: ${reservationRes.status} - ${reservationRes.body}`);
      }
    });

    sleep(1);

    // 3. View own reservations
    group('3. Student views own reservations', () => {
      const reservationsRes = http.get(
        `${BASE_URL}/api/reservations/me`,
        { headers: studentHeaders }
      );

      check(reservationsRes, {
        'student can view their own reservations': (r) => r.status === 200,
        'student reservations response is an array': (r) => Array.isArray(r.json()),
        'student reservations list contains the created reservation': (r) => 
          Array.isArray(r.json()) && r.json().some(res => res.id === reservations.mainReservationId),
      });

      const count = Array.isArray(reservationsRes.json()) ? reservationsRes.json().length : 0;
      console.log(`  ✓ Student has ${count} reservations`);
    });

    sleep(1);

    // 4. View pending reservations
    group('4. Student views pending reservations', () => {
      const pendingRes = http.get(
        `${BASE_URL}/api/reservations/me?status=PENDING`,
        { headers: studentHeaders }
      );

      check(pendingRes, {
        'student can filter reservations by PENDING status': (r) => r.status === 200,
        'student has at least one pending reservation': (r) => 
          Array.isArray(r.json()) && r.json().length > 0,
      });

      const count = Array.isArray(pendingRes.json()) ? pendingRes.json().length : 0;
      console.log(`  ✓ Student has ${count} pending reservations`);
    });

    sleep(1);

    // 5. Create multiple concurrent reservations for different rooms
    group('5. Student creates concurrent reservations', () => {
      const concurrentTime = new Date(Date.now() + 168 * 60 * 60 * 1000); // 7 days ahead
      const concurrentEndTime = new Date(concurrentTime.getTime() + 1 * 60 * 60 * 1000);

      // Create reservations for all available rooms
      for (let i = 0; i < data.rooms.length; i++) {
        const reservationRes = http.post(
          `${BASE_URL}/api/reservations`,
          JSON.stringify({
            roomId: data.rooms[i],
            startDateTime: concurrentTime.toISOString().slice(0, 19),
            endDateTime: concurrentEndTime.toISOString().slice(0, 19),
            attendees: 5 + i * 2,
          }),
          { headers: studentHeaders }
        );

        const success = check(reservationRes, {
          [`student can create concurrent reservation for room ${i + 1}`]: (r) => r.status === 201 || r.status === 200,
          [`concurrent reservation ${i + 1} has an ID`]: (r) => r.json('id') !== undefined,
        });

        if (success) {
          reservations.concurrentReservationIds.push(reservationRes.json('id'));
        }

        sleep(0.3);
      }

      check(reservations.concurrentReservationIds, {
        'multiple concurrent reservations created': (ids) => ids.length === data.rooms.length,
      });

      console.log(`  ✓ Created ${reservations.concurrentReservationIds.length} concurrent reservations`);
    });
  });

  workflowDuration.add(Date.now() - workflowStart);
  studentReservationWorkflows.add(1);
  console.log('=== STUDENT RESERVATIONS WORKFLOW COMPLETE ===\n');

  return reservations;
}
