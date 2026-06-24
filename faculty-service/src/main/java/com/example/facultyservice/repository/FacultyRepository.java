package com.example.facultyservice.repository;
import com.example.facultyservice.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface FacultyRepository extends JpaRepository<Faculty, String> {
}