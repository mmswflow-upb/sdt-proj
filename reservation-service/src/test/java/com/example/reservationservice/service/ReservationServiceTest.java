package com.example.reservationservice.service;

import com.example.reservationservice.dto.ReservationRequestDto;
import com.example.reservationservice.dto.ScheduleRequest;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import com.example.reservationservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository repository;

    @Mock
    private SchedulingClient schedulingClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private ReservationService service;

    private ReservationRequestDto validRequest;
    private String userId;
    private String roomId;
    private LocalDateTime futureStart;
    private LocalDateTime futureEnd;

    @BeforeEach
    void setUp() {
        userId = "user123";
        roomId = "ROOM-001";
        futureStart = LocalDateTime.now().plusHours(2);
        futureEnd = LocalDateTime.now().plusHours(3);

        validRequest = new ReservationRequestDto();
        validRequest.setRoomId(roomId);
        validRequest.setStartDateTime(futureStart);
        validRequest.setEndDateTime(futureEnd);
        validRequest.setAttendees(5);
        validRequest.setEquipment(Arrays.asList("Projector", "Whiteboard"));
    }

    private void setId(Reservation reservation, Long id) throws Exception {
        Field idField = Reservation.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(reservation, id);
    }

    @Test
    void createReservation_Success() throws Exception {
        // Arrange
        when(repository.findDuplicateReservation(userId, roomId, futureStart, futureEnd))
                .thenReturn(Collections.emptyList());
        when(schedulingClient.isAvailable(roomId, futureStart, futureEnd)).thenReturn(true);
        
        Reservation savedReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, "Projector,Whiteboard", ReservationStatus.PENDING);
        setId(savedReservation, 1L);
        when(repository.save(any(Reservation.class))).thenReturn(savedReservation);

        // Act
        Reservation result = service.createReservation(validRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.PENDING, result.getStatus());
        assertEquals(userId, result.getUserId());
        verify(schedulingClient).createSchedule(any(ScheduleRequest.class));
        verify(notificationPublisher).publishReservationCreated(
                eq(1L), eq(userId), eq(roomId), eq(futureStart), eq(futureEnd), eq("PENDING"));
    }

    @Test
    void createReservation_ThrowsException_WhenStartTimeIsInThePast() {
        // Arrange
        LocalDateTime pastStart = LocalDateTime.now().minusHours(1);
        validRequest.setStartDateTime(pastStart);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                service.createReservation(validRequest, userId));
        verify(repository, never()).save(any());
    }

    @Test
    void createReservation_ThrowsException_WhenEndTimeBeforeStartTime() {
        // Arrange
        validRequest.setEndDateTime(futureStart.minusHours(1));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                service.createReservation(validRequest, userId));
        verify(repository, never()).save(any());
    }

    @Test
    void createReservation_ThrowsException_WhenDuplicateReservationExists() {
        // Arrange
        Reservation existingReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        when(repository.findDuplicateReservation(userId, roomId, futureStart, futureEnd))
                .thenReturn(Collections.singletonList(existingReservation));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                service.createReservation(validRequest, userId));
        verify(schedulingClient, never()).isAvailable(anyString(), any(), any());
    }

    @Test
    void createReservation_ThrowsException_WhenRoomNotAvailable() {
        // Arrange
        when(repository.findDuplicateReservation(userId, roomId, futureStart, futureEnd))
                .thenReturn(Collections.emptyList());
        when(schedulingClient.isAvailable(roomId, futureStart, futureEnd)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                service.createReservation(validRequest, userId));
        verify(repository, never()).save(any());
    }

    @Test
    void approveReservation_Success() throws Exception {
        // Arrange
        Reservation pendingReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        setId(pendingReservation, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(pendingReservation));
        when(repository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = service.approveReservation(1L);

        // Assert
        assertEquals(ReservationStatus.APPROVED, result.getStatus());
        verify(notificationPublisher).publishReservationApproved(
                eq(1L), eq(userId), eq(roomId), eq("APPROVED"));
    }

    @Test
    void approveReservation_ThrowsException_WhenReservationNotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                service.approveReservation(999L));
    }

    @Test
    void approveReservation_ThrowsException_WhenReservationCancelled() throws Exception {
        // Arrange
        Reservation cancelledReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.CANCELLED);
        setId(cancelledReservation, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(cancelledReservation));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                service.approveReservation(1L));
        verify(repository, never()).save(any());
    }

    @Test
    void cancelReservation_Success() throws Exception {
        // Arrange
        Reservation pendingReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        setId(pendingReservation, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(pendingReservation));
        when(repository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = service.cancelReservation(1L, userId);

        // Assert
        assertEquals(ReservationStatus.CANCELLED, result.getStatus());
        verify(schedulingClient).removeScheduleForReservation(1L);
        verify(notificationPublisher).publishReservationCancelled(
                eq(1L), eq(userId), eq(roomId), eq("CANCELLED"));
    }

    @Test
    void cancelReservation_ThrowsException_WhenUserDoesNotOwnReservation() throws Exception {
        // Arrange
        Reservation reservation = new Reservation("otherUser", roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        setId(reservation, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                service.cancelReservation(1L, userId));
        verify(repository, never()).save(any());
    }

    @Test
    void revokeReservation_Success() throws Exception {
        // Arrange
        Reservation approvedReservation = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.APPROVED);
        setId(approvedReservation, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(approvedReservation));
        when(repository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = service.revokeReservation(1L);

        // Assert
        assertEquals(ReservationStatus.REVOKED, result.getStatus());
        verify(schedulingClient).removeScheduleForReservation(1L);
        verify(notificationPublisher).publishReservationRevoked(
                eq(1L), eq(userId), eq(roomId), eq("REVOKED"));
    }

    @Test
    void getReservationsForUser_ReturnsUserReservations() {
        // Arrange
        Reservation res1 = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        Reservation res2 = new Reservation(userId, "ROOM-002", futureStart.plusDays(1), futureEnd.plusDays(1), 
                3, null, ReservationStatus.APPROVED);

        when(repository.findByUserId(userId)).thenReturn(Arrays.asList(res1, res2));

        // Act
        List<Reservation> result = service.getReservationsForUser(userId);

        // Assert
        assertEquals(2, result.size());
        verify(repository).findByUserId(userId);
    }

    @Test
    void getReservationsForUserByStatus_FiltersCorrectly() {
        // Arrange
        Reservation pending = new Reservation(userId, roomId, futureStart, futureEnd, 
                5, null, ReservationStatus.PENDING);
        Reservation approved = new Reservation(userId, "ROOM-002", futureStart.plusDays(1), futureEnd.plusDays(1), 
                3, null, ReservationStatus.APPROVED);

        when(repository.findByUserId(userId)).thenReturn(Arrays.asList(pending, approved));

        // Act
        List<Reservation> result = service.getReservationsForUserByStatus(userId, ReservationStatus.APPROVED);

        // Assert
        assertEquals(1, result.size());
        assertEquals(ReservationStatus.APPROVED, result.get(0).getStatus());
    }
}
