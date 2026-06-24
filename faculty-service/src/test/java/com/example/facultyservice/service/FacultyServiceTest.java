package com.example.facultyservice.service;

import com.example.facultyservice.entity.Faculty;
import com.example.facultyservice.entity.FacultyRoom;
import com.example.facultyservice.repository.FacultyRepository;
import com.example.facultyservice.repository.FacultyRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyRoomRepository facultyRoomRepository;

    @Mock
    private FacultyRoomService facultyRoomService;

    @InjectMocks
    private FacultyService facultyService;

    private String facultyId;
    private String facultyName;
    private Faculty faculty;

    @BeforeEach
    void setUp() {
        facultyId = "FAC-001";
        facultyName = "Engineering Faculty";
        faculty = new Faculty(facultyId, facultyName);
    }

    @Test
    void createFaculty_Success() {
        // Arrange
        when(facultyRepository.existsById(facultyId)).thenReturn(false);
        when(facultyRepository.save(any(Faculty.class))).thenReturn(faculty);

        // Act
        Faculty result = facultyService.createFaculty(facultyId, facultyName);

        // Assert
        assertNotNull(result);
        assertEquals(facultyId, result.getFacultyId());
        assertEquals(facultyName, result.getName());
        verify(facultyRepository).existsById(facultyId);
        verify(facultyRepository).save(any(Faculty.class));
    }

    @Test
    void createFaculty_ThrowsException_WhenFacultyAlreadyExists() {
        // Arrange
        when(facultyRepository.existsById(facultyId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                facultyService.createFaculty(facultyId, facultyName));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(facultyRepository, never()).save(any());
    }

    @Test
    void updateFaculty_Success() {
        // Arrange
        String newName = "Updated Faculty Name";
        when(facultyRepository.findById(facultyId)).thenReturn(Optional.of(faculty));
        when(facultyRepository.save(any(Faculty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Faculty result = facultyService.updateFaculty(facultyId, newName);

        // Assert
        assertEquals(newName, result.getName());
        verify(facultyRepository).findById(facultyId);
        verify(facultyRepository).save(any(Faculty.class));
    }

    @Test
    void updateFaculty_ThrowsException_WhenFacultyNotFound() {
        // Arrange
        when(facultyRepository.findById(facultyId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                facultyService.updateFaculty(facultyId, "New Name"));

        assertTrue(exception.getMessage().contains("Faculty not found"));
        verify(facultyRepository, never()).save(any());
    }

    @Test
    void deleteFaculty_Success_WithNoRooms() {
        // Arrange
        when(facultyRepository.existsById(facultyId)).thenReturn(true);
        when(facultyRoomRepository.findByFacultyId(facultyId)).thenReturn(Collections.emptyList());
        doNothing().when(facultyRepository).deleteById(facultyId);

        // Act
        facultyService.deleteFaculty(facultyId);

        // Assert
        verify(facultyRepository).existsById(facultyId);
        verify(facultyRoomRepository).findByFacultyId(facultyId);
        verify(facultyRepository).deleteById(facultyId);
        verify(facultyRoomService, never()).deleteRoom(anyString());
    }

    @Test
    void deleteFaculty_Success_DeletesAssociatedRooms() {
        // Arrange
        FacultyRoom room1 = new FacultyRoom();
        room1.setRoomId("ROOM-001");
        room1.setFacultyId(facultyId);

        FacultyRoom room2 = new FacultyRoom();
        room2.setRoomId("ROOM-002");
        room2.setFacultyId(facultyId);

        when(facultyRepository.existsById(facultyId)).thenReturn(true);
        when(facultyRoomRepository.findByFacultyId(facultyId)).thenReturn(Arrays.asList(room1, room2));
        doNothing().when(facultyRoomService).deleteRoom(anyString());
        doNothing().when(facultyRepository).deleteById(facultyId);

        // Act
        facultyService.deleteFaculty(facultyId);

        // Assert
        verify(facultyRoomService).deleteRoom("ROOM-001");
        verify(facultyRoomService).deleteRoom("ROOM-002");
        verify(facultyRepository).deleteById(facultyId);
    }

    @Test
    void deleteFaculty_ThrowsException_WhenFacultyNotFound() {
        // Arrange
        when(facultyRepository.existsById(facultyId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                facultyService.deleteFaculty(facultyId));

        assertTrue(exception.getMessage().contains("Faculty not found"));
        verify(facultyRepository, never()).deleteById(anyString());
        verify(facultyRoomService, never()).deleteRoom(anyString());
    }

    @Test
    void deleteFaculty_ContinuesAfterRoomDeletionFailure() {
        // Arrange
        FacultyRoom room1 = new FacultyRoom();
        room1.setRoomId("ROOM-001");
        room1.setFacultyId(facultyId);

        FacultyRoom room2 = new FacultyRoom();
        room2.setRoomId("ROOM-002");
        room2.setFacultyId(facultyId);

        when(facultyRepository.existsById(facultyId)).thenReturn(true);
        when(facultyRoomRepository.findByFacultyId(facultyId)).thenReturn(Arrays.asList(room1, room2));
        doThrow(new RuntimeException("Room deletion failed")).when(facultyRoomService).deleteRoom("ROOM-001");
        doNothing().when(facultyRoomService).deleteRoom("ROOM-002");
        doNothing().when(facultyRepository).deleteById(facultyId);

        // Act
        facultyService.deleteFaculty(facultyId);

        // Assert
        verify(facultyRoomService).deleteRoom("ROOM-001");
        verify(facultyRoomService).deleteRoom("ROOM-002");
        verify(facultyRepository).deleteById(facultyId);
    }

    @Test
    void getFaculty_ReturnsData_WhenFacultyExists() {
        // Arrange
        when(facultyRepository.findById(facultyId)).thenReturn(Optional.of(faculty));

        // Act
        Optional<Faculty> result = facultyService.getFaculty(facultyId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(facultyId, result.get().getFacultyId());
        assertEquals(facultyName, result.get().getName());
        verify(facultyRepository).findById(facultyId);
    }

    @Test
    void getFaculty_ReturnsEmpty_WhenFacultyNotFound() {
        // Arrange
        when(facultyRepository.findById(facultyId)).thenReturn(Optional.empty());

        // Act
        Optional<Faculty> result = facultyService.getFaculty(facultyId);

        // Assert
        assertFalse(result.isPresent());
        verify(facultyRepository).findById(facultyId);
    }

    @Test
    void getAllFaculties_ReturnsAllFaculties() {
        // Arrange
        Faculty faculty1 = new Faculty("FAC-001", "Engineering");
        Faculty faculty2 = new Faculty("FAC-002", "Medicine");
        
        when(facultyRepository.findAll()).thenReturn(Arrays.asList(faculty1, faculty2));

        // Act
        List<Faculty> result = facultyService.getAllFaculties();

        // Assert
        assertEquals(2, result.size());
        verify(facultyRepository).findAll();
    }

    @Test
    void getAllFaculties_ReturnsEmptyList_WhenNoFacultiesExist() {
        // Arrange
        when(facultyRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Faculty> result = facultyService.getAllFaculties();

        // Assert
        assertTrue(result.isEmpty());
        verify(facultyRepository).findAll();
    }
}
