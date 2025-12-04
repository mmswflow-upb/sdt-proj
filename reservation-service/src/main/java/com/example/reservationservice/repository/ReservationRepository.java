package com.example.reservationservice.repository;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByRoomId(String roomId);
    @Transactional
    @Modifying
    @Query("update Reservation r set r.status = ?2 where r.id = ?1")
    void updateStatus(Long id, ReservationStatus status);
}