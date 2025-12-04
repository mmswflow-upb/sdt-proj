package com.example.schedulingservice.repository;
import com.example.schedulingservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
}