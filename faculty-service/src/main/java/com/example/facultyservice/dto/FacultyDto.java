package com.example.facultyservice.dto;
import jakarta.validation.constraints.NotBlank;
public class FacultyDto {
    @NotBlank
    private String facultyId;
    @NotBlank
    private String name;
    public FacultyDto() {}
    public FacultyDto(String facultyId, String name) {
        this.facultyId = facultyId;
        this.name = name;
    }
    public String getFacultyId() {
        return facultyId;
    }
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}