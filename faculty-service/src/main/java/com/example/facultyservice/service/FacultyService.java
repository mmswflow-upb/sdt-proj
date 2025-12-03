package com.example.facultyservice.service;

import com.example.facultyservice.entity.Faculty;
import com.example.facultyservice.entity.FacultyRoom;
import com.example.facultyservice.repository.FacultyRepository;
import com.example.facultyservice.repository.FacultyRoomRepository;
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
    private final FacultyRoomRepository facultyRoomRepository;
    private final FacultyRoomService facultyRoomService;

    public FacultyService(FacultyRepository facultyRepository, 
                          FacultyRoomRepository facultyRoomRepository,
                          FacultyRoomService facultyRoomService) {
        this.facultyRepository = facultyRepository;
        this.facultyRoomRepository = facultyRoomRepository;
        this.facultyRoomService = facultyRoomService;
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
        
        // Find all rooms belonging to this faculty and delete them (which cascades to reservations/schedules)
        List<String> roomIds = facultyRoomRepository.findByFacultyId(facultyId)
            .stream()
            .map(room -> room.getRoomId())
            .toList();
        
        // Delete each room (this will cascade to scheduling-service and reservation-service)
        for (String roomId : roomIds) {
            try {
                facultyRoomService.deleteRoom(roomId);
            } catch (Exception e) {
                // Log and continue - we want to delete as much as possible
                System.err.println("Failed to delete room " + roomId + " during faculty deletion: " + e.getMessage());
            }
        }
        
        // Finally delete the faculty itself
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