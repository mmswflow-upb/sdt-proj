package com.example.schedulingservice.service;
import com.example.schedulingservice.entity.Room;
import com.example.schedulingservice.entity.RoomSchedule;
import com.example.schedulingservice.repository.RoomRepository;
import com.example.schedulingservice.repository.RoomScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class SchedulingService {
    private final RoomRepository roomRepository;
    private final RoomScheduleRepository scheduleRepository;
    public SchedulingService(RoomRepository roomRepository, RoomScheduleRepository scheduleRepository) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
    }
    @Transactional(readOnly = true)
    public boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return false;
        }
        List<RoomSchedule> schedules = scheduleRepository.findByRoomId(roomId);
        for (RoomSchedule sched : schedules) {
            boolean noOverlap = end.isBefore(sched.getStartDateTime()) || end.isEqual(sched.getStartDateTime()) ||
                               start.isAfter(sched.getEndDateTime()) || start.isEqual(sched.getEndDateTime());
            if (!noOverlap) {
                return false;  // Found an overlap
            }
        }
        return true;
    }
    @Transactional
    public void createSchedule(String roomId, LocalDateTime start, LocalDateTime end, Long reservationId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        RoomSchedule schedule = new RoomSchedule(roomId, start, end, reservationId);
        scheduleRepository.save(schedule);
    }
    @Transactional
    public void removeSchedulesForRoom(String roomId) {
        scheduleRepository.deleteByRoomId(roomId);
    }
    @Transactional
    public void removeSchedulesForReservation(Long reservationId) {
        scheduleRepository.deleteByReservationId(reservationId);
    }
}