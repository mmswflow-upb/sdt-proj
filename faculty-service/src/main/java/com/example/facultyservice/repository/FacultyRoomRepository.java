package com.example.facultyservice.repository;

import com.example.facultyservice.entity.FacultyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacultyRoomRepository extends JpaRepository<FacultyRoom, String> {
}