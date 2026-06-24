package com.example.facultyservice.repository;
import com.example.facultyservice.entity.FacultyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface FacultyRoomRepository extends JpaRepository<FacultyRoom, String> {
    List<FacultyRoom> findByFacultyId(String facultyId);
}