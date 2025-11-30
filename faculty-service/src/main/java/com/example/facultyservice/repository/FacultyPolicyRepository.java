package com.example.facultyservice.repository;

import com.example.facultyservice.entity.FacultyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyPolicyRepository extends JpaRepository<FacultyPolicy, Long> {
    Optional<FacultyPolicy> findByFacultyId(String facultyId);
}