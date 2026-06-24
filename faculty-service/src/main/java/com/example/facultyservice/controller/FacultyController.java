package com.example.facultyservice.controller;
import com.example.facultyservice.dto.FacultyDto;
import com.example.facultyservice.entity.Faculty;
import com.example.facultyservice.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/faculties")
public class FacultyController {
    private final FacultyService facultyService;
    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Faculty> create(@Valid @RequestBody FacultyDto dto) {
        Faculty faculty = facultyService.createFaculty(dto.getFacultyId(), dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(faculty);
    }
    @GetMapping
    public ResponseEntity<List<Faculty>> all() {
        return ResponseEntity.ok(facultyService.getAllFaculties());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Faculty> get(@PathVariable String id) {
        Optional<Faculty> faculty = facultyService.getFaculty(id);
        return faculty.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Faculty> update(@PathVariable String id, @Valid @RequestBody FacultyDto dto) {
        try {
            Faculty updated = facultyService.updateFaculty(id, dto.getName());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            facultyService.deleteFaculty(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}