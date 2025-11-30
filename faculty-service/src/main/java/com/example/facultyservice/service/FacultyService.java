package com.example.facultyservice.service;

import com.example.facultyservice.entity.Faculty;
import com.example.facultyservice.repository.FacultyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service encapsulating CRUD operations for faculties. Only administrators should
 * modify faculties; other users may read them.
 */
@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    @Transactional
    public Faculty createFaculty(String facultyId, String name) {
        if (facultyRepository.existsById(facultyId)) {
            throw new IllegalStateException("Faculty with id " + facultyId + " already exists");
        }
        Faculty faculty = new Faculty(facultyId, name);
        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty updateFaculty(String facultyId, String name) {
        return facultyRepository.findById(facultyId)
                .map(faculty -> {
                    faculty.setName(name);
                    return facultyRepository.save(faculty);
                })
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + facultyId));
    }

    @Transactional
    public void deleteFaculty(String facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new IllegalArgumentException("Faculty not found: " + facultyId);
        }
        facultyRepository.deleteById(facultyId);
    }

    @Transactional(readOnly = true)
    public Optional<Faculty> getFaculty(String facultyId) {
        return facultyRepository.findById(facultyId);
    }

    @Transactional(readOnly = true)
    public List<Faculty> getAllFaculties() {
        return facultyRepository.findAll();
    }
}