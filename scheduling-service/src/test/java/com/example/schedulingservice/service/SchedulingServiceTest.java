package com.example.schedulingservice.service;

import com.example.schedulingservice.entity.Room;
import com.example.schedulingservice.entity.RoomSchedule;
import com.example.schedulingservice.repository.RoomRepository;
import com.example.schedulingservice.repository.RoomScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomScheduleRepository scheduleRepository;

    @InjectMocks
    private SchedulingService schedulingService;

    private String roomId;
    private Room room;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        roomId = "ROOM-001";
        room = new Room();
        room.setRoomId(roomId);
        room.setFacultyId("FAC-001");
        room.setCapacity(30);

        start = LocalDateTime.of(2024, 12, 20, 10, 0);
        end = LocalDateTime.of(2024, 12, 20, 11, 0);
    }

    @Test
    void isAvailable_ReturnsTrue_WhenNoSchedulesExist() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.emptyList());

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertTrue(result);
        verify(roomRepository).findById(roomId);
        verify(scheduleRepository).findByRoomId(roomId);
    }

    @Test
    void isAvailable_ReturnsTrue_WhenSchedulesDoNotOverlap() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2024, 12, 20, 8, 0);
        LocalDateTime existingEnd = LocalDateTime.of(2024, 12, 20, 9, 0);
        RoomSchedule existingSchedule = new RoomSchedule(roomId, existingStart, existingEnd, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.singletonList(existingSchedule));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertTrue(result);
    }

    @Test
    void isAvailable_ReturnsFalse_WhenSchedulesOverlap() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2024, 12, 20, 9, 30);
        LocalDateTime existingEnd = LocalDateTime.of(2024, 12, 20, 10, 30);
        RoomSchedule existingSchedule = new RoomSchedule(roomId, existingStart, existingEnd, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.singletonList(existingSchedule));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAvailable_ReturnsFalse_WhenNewScheduleContainsExisting() {
        // Arrange
        LocalDateTime existingStart = LocalDateTime.of(2024, 12, 20, 10, 15);
        LocalDateTime existingEnd = LocalDateTime.of(2024, 12, 20, 10, 45);
        RoomSchedule existingSchedule = new RoomSchedule(roomId, existingStart, existingEnd, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.singletonList(existingSchedule));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAvailable_ReturnsTrue_WhenEndTimeEqualsExistingStartTime() {
        // Arrange
        LocalDateTime existingStart = end;
        LocalDateTime existingEnd = end.plusHours(1);
        RoomSchedule existingSchedule = new RoomSchedule(roomId, existingStart, existingEnd, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.singletonList(existingSchedule));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertTrue(result);
    }

    @Test
    void isAvailable_ReturnsTrue_WhenStartTimeEqualsExistingEndTime() {
        // Arrange
        LocalDateTime existingStart = start.minusHours(1);
        LocalDateTime existingEnd = start;
        RoomSchedule existingSchedule = new RoomSchedule(roomId, existingStart, existingEnd, 1L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Collections.singletonList(existingSchedule));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertTrue(result);
    }

    @Test
    void isAvailable_ThrowsException_WhenRoomNotFound() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schedulingService.isAvailable(roomId, start, end));

        assertTrue(exception.getMessage().contains("Room not found"));
        verify(scheduleRepository, never()).findByRoomId(anyString());
    }

    @Test
    void isAvailable_HandlesMultipleSchedules() {
        // Arrange
        RoomSchedule schedule1 = new RoomSchedule(roomId, 
                LocalDateTime.of(2024, 12, 20, 8, 0),
                LocalDateTime.of(2024, 12, 20, 9, 0), 1L);
        RoomSchedule schedule2 = new RoomSchedule(roomId, 
                LocalDateTime.of(2024, 12, 20, 12, 0),
                LocalDateTime.of(2024, 12, 20, 13, 0), 2L);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByRoomId(roomId)).thenReturn(Arrays.asList(schedule1, schedule2));

        // Act
        boolean result = schedulingService.isAvailable(roomId, start, end);

        // Assert
        assertTrue(result);
    }

    @Test
    void createSchedule_Success() {
        // Arrange
        Long reservationId = 100L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(scheduleRepository.save(any(RoomSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        schedulingService.createSchedule(roomId, start, end, reservationId);

        // Assert
        verify(roomRepository).findById(roomId);
        verify(scheduleRepository).save(any(RoomSchedule.class));
    }

    @Test
    void createSchedule_ThrowsException_WhenRoomNotFound() {
        // Arrange
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schedulingService.createSchedule(roomId, start, end, 100L));

        assertTrue(exception.getMessage().contains("Room not found"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void removeSchedulesForRoom_Success() {
        // Arrange
        doNothing().when(scheduleRepository).deleteByRoomId(roomId);

        // Act
        schedulingService.removeSchedulesForRoom(roomId);

        // Assert
        verify(scheduleRepository).deleteByRoomId(roomId);
    }

    @Test
    void removeSchedulesForReservation_Success() {
        // Arrange
        Long reservationId = 100L;
        doNothing().when(scheduleRepository).deleteByReservationId(reservationId);

        // Act
        schedulingService.removeSchedulesForReservation(reservationId);

        // Assert
        verify(scheduleRepository).deleteByReservationId(reservationId);
    }
}
