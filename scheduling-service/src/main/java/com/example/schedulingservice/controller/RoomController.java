package com.example.schedulingservice.controller;

import com.example.schedulingservice.entity.Room;
import com.example.schedulingservice.repository.RoomRepository;
import com.example.schedulingservice.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing CRUD operations for rooms. The faculty-service uses these endpoints to
 * create, update, delete or lock rooms. All operations require authentication and certain operations
 * require admin privileges.
 */
@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final SchedulingService schedulingService;

    public RoomController(RoomRepository roomRepository, SchedulingService schedulingService) {
        this.roomRepository = roomRepository;
        this.schedulingService = schedulingService;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PostMapping
    public ResponseEntity<Room> create(@Valid @RequestBody Room room) {
        if (roomRepository.existsById(room.getRoomId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Room saved = roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Room>> all() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> get(@PathVariable String id) {
        Optional<Room> room = roomRepository.findById(id);
        return room.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Room> update(@PathVariable String id, @Valid @RequestBody Room updated) {
        return roomRepository.findById(id).map(room -> {
            room.setCapacity(updated.getCapacity());
            room.setEquipment(updated.getEquipment());
            room.setFacultyId(updated.getFacultyId());
            room.setLocked(updated.isLocked());
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a room. Also removes any schedules associated with it. Used by the faculty-service
     * when an admin or faculty admin deletes a room. Only admins and faculty admins may perform this operation.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        return roomRepository.findById(id).map(room -> {
            schedulingService.removeSchedulesForRoom(id);
            roomRepository.delete(room);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Locks a room, preventing new reservations and clearing existing schedules. Admin and faculty admin only.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PostMapping("/{id}/lock")
    public ResponseEntity<Room> lock(@PathVariable String id) {
        try {
            Room locked = schedulingService.lockRoom(id);
            return ResponseEntity.ok(locked);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Unlocks a previously locked room. Admin and faculty admin only.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Room> unlock(@PathVariable String id) {
        try {
            Room unlocked = schedulingService.unlockRoom(id);
            return ResponseEntity.ok(unlocked);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}