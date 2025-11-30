package com.example.facultyservice.entity;

import jakarta.persistence.*;

/**
 * Entity representing policies for a given faculty. Policies influence how reservations are validated
 * (e.g. maximum duration, whether approval is required and which roles are allowed to create reservations).
 */
@Entity
@Table(name = "faculty_policies")
public class FacultyPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String facultyId;
    /**
     * Maximum duration of a reservation in minutes.
     */
    private int maxDuration;
    /**
     * Whether reservations require admin approval.
     */
    private boolean requireApproval;
    /**
     * Comma-separated list of roles allowed to submit reservations without further approval.
     */
    private String allowedRoles;

    public FacultyPolicy() {}

    public FacultyPolicy(String facultyId, int maxDuration, boolean requireApproval, String allowedRoles) {
        this.facultyId = facultyId;
        this.maxDuration = maxDuration;
        this.requireApproval = requireApproval;
        this.allowedRoles = allowedRoles;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFacultyId() {
        return facultyId;
    }
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }
    public int getMaxDuration() {
        return maxDuration;
    }
    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }
    public boolean isRequireApproval() {
        return requireApproval;
    }
    public void setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
    }
    public String getAllowedRoles() {
        return allowedRoles;
    }
    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }
}