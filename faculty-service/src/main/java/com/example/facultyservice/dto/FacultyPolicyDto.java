package com.example.facultyservice.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public class FacultyPolicyDto {
    private Long id;
    @NotBlank
    private String facultyId;
    @NotNull
    private Integer maxDuration;
    @NotNull
    private Boolean requireApproval;
    private String allowedRoles;
    public FacultyPolicyDto() {}
    public FacultyPolicyDto(Long id, String facultyId, Integer maxDuration, Boolean requireApproval, String allowedRoles) {
        this.id = id;
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
    public Integer getMaxDuration() {
        return maxDuration;
    }
    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }
    public Boolean getRequireApproval() {
        return requireApproval;
    }
    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }
    public String getAllowedRoles() {
        return allowedRoles;
    }
    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }
}