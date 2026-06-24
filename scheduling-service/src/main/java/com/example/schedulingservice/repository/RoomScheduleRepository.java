package com.example.schedulingservice.repository;
import com.example.schedulingservice.entity.RoomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Repository
public interface RoomScheduleRepository extends JpaRepository<RoomSchedule, Long> {
    List<RoomSchedule> findByRoomId(String roomId);
    @Transactional
    void deleteByRoomId(String roomId);
    @Transactional
    void deleteByReservationId(Long reservationId);
}