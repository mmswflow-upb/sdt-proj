package com.example.facultyservice.controller;

import com.example.facultyservice.dto.FacultyPolicyDto;
import com.example.facultyservice.entity.FacultyPolicy;
import com.example.facultyservice.service.FacultyPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller exposing CRUD operations for faculty policies. Only administrators may create,
 * update or delete policies. All authenticated users may read policies.
 */
@RestController
@RequestMapping("/policies")
public class FacultyPolicyController {

    private final FacultyPolicyService policyService;

    public FacultyPolicyController(FacultyPolicyService policyService) {
        this.policyService = policyService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FacultyPolicy> create(@Valid @RequestBody FacultyPolicyDto dto) {
        FacultyPolicy policy = policyService.createPolicy(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(policy);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FacultyPolicy> update(@PathVariable Long id, @Valid @RequestBody FacultyPolicyDto dto) {
        try {
            FacultyPolicy updated = policyService.updatePolicy(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FacultyPolicy>> all() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyPolicy> get(@PathVariable Long id) {
        Optional<FacultyPolicy> policy = policyService.getPolicy(id);
        return policy.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            policyService.deletePolicy(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}