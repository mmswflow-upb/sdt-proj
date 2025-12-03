package com.example.schedulingservice.service;

import com.example.schedulingservice.entity.Room;
import com.example.schedulingservice.entity.RoomSchedule;
import com.example.schedulingservice.repository.RoomRepository;
import com.example.schedulingservice.repository.RoomScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service responsible for scheduling operations such as checking availability, creating
 * or removing schedule entries and locking/unlocking rooms. Business rules for overlapping time
 * slots are enforced here.
 */
@Service
public class SchedulingService {

    private final RoomRepository roomRepository;
    private final RoomScheduleRepository scheduleRepository;

    public SchedulingService(RoomRepository roomRepository, RoomScheduleRepository scheduleRepository) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Returns true if the specified room is available during the provided time range. A room is considered
     * unavailable if it is locked or if any existing schedule overlaps with the requested slot.
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.isLocked()) {
            return false;
        }
        List<RoomSchedule> schedules = scheduleRepository.findByRoomId(roomId);
        for (RoomSchedule sched : schedules) {
            // Intervals DON'T overlap if one ends before/at the start of the other
            // For back-to-back: if new starts when old ends, they don't overlap
            boolean noOverlap = end.isBefore(sched.getStartDateTime()) || end.isEqual(sched.getStartDateTime()) ||
                               start.isAfter(sched.getEndDateTime()) || start.isEqual(sched.getEndDateTime());
            if (!noOverlap) {
                return false;  // Found an overlap
            }
        }
        return true;
    }

    /**
     * Creates a schedule entry to block the given room during the specified time range. If the room does not
     * exist, an IllegalArgumentException is thrown. The reservationId may be null for ad-hoc blocking.
     */
    @Transactional
    public void createSchedule(String roomId, LocalDateTime start, LocalDateTime end, Long reservationId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        RoomSchedule schedule = new RoomSchedule(roomId, start, end, reservationId);
        scheduleRepository.save(schedule);
    }

    /**
     * Removes all schedules associated with the given room.
     */
    @Transactional
    public void removeSchedulesForRoom(String roomId) {
        scheduleRepository.deleteByRoomId(roomId);
    }

    /**
     * Removes schedule entries tied to a specific reservation. Used when a reservation is revoked.
     */
    @Transactional
    public void removeSchedulesForReservation(Long reservationId) {
        scheduleRepository.deleteByReservationId(reservationId);
    }

    /**
     * Locks a room to prevent any new schedules and removes existing schedules. Typically invoked when
     * an admin marks a room as temporarily unavailable. Throws IllegalArgumentException if the room does not exist.
     */
    @Transactional
    public Room lockRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (!room.isLocked()) {
            room.setLocked(true);
            removeSchedulesForRoom(roomId);
        }
        return roomRepository.save(room);
    }

    /**
     * Unlocks a previously locked room. Throws IllegalArgumentException if the room does not exist.
     */
    @Transactional
    public Room unlockRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        if (room.isLocked()) {
            room.setLocked(false);
        }
        return roomRepository.save(room);
    }
}