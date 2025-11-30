package com.example.facultyservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing an academic faculty (e.g. Engineering, Science). The identifier is a human-friendly
 * code such as "ENG" and is used to group users and rooms.
 */
@Entity
@Table(name = "faculties")
public class Faculty {
    @Id
    @Column(name = "faculty_id")
    private String facultyId;
    @Column(nullable = false)
    private String name;

    public Faculty() {}

    public Faculty(String facultyId, String name) {
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