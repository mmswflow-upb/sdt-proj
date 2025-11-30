package com.example.facultyservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used to create a new user account. Guests may omit the role field in which case
 * the default role STUDENT will be used. The facultyId associates the user with a particular
 * faculty.
 */
public class RegisterRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String role;
    @NotBlank
    private String facultyId;

    public RegisterRequestDto() {}

    public RegisterRequestDto(String username, String password, String role, String facultyId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.facultyId = facultyId;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getFacultyId() {
        return facultyId;
    }
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }
}