package com.example.reservationservice.repository;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByRoomId(String roomId);
    @Query("SELECT r FROM Reservation r WHERE r.userId = ?1 AND r.roomId = ?2 AND r.startDateTime = ?3 AND r.endDateTime = ?4 AND r.status != 'CANCELLED' AND r.status != 'REVOKED'")
    List<Reservation> findDuplicateReservation(String userId, String roomId, LocalDateTime start, LocalDateTime end);
    @Transactional
    @Modifying
    @Query("update Reservation r set r.status = ?2 where r.id = ?1")
    void updateStatus(Long id, ReservationStatus status);
}