package com.example.facultyservice.controller;

import com.example.facultyservice.dto.FacultyRoomDto;
import com.example.facultyservice.entity.FacultyRoom;
import com.example.facultyservice.service.FacultyRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing CRUD operations for rooms owned by faculties. Only administrators
 * may create, update, delete or lock/unlock rooms. All authenticated users may list rooms
 * and fetch individual room details.
 */
@RestController
@RequestMapping("/rooms")
public class FacultyRoomController {

    private final FacultyRoomService roomService;

    public FacultyRoomController(FacultyRoomService roomService) {
        this.roomService = roomService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FacultyRoom> create(@Valid @RequestBody FacultyRoomDto dto) {
        try {
            FacultyRoom room = roomService.createRoom(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FacultyRoom>> all() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyRoom> get(@PathVariable String id) {
        Optional<FacultyRoom> room = roomService.getRoom(id);
        return room.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FacultyRoom> update(@PathVariable String id, @Valid @RequestBody FacultyRoomDto dto) {
        try {
            FacultyRoom updated = roomService.updateRoom(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/lock")
    public ResponseEntity<Void> lock(@PathVariable String id) {
        try {
            roomService.lockRoom(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable String id) {
        try {
            roomService.unlockRoom(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}