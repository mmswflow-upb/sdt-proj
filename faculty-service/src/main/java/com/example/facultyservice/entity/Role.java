package com.example.facultyservice.entity;

/**
 * Enumeration of roles assigned to users. Roles are stored as simple strings within JWT tokens and
 * used by Spring Security for access control.
 */
public enum Role {
    STUDENT,
    FACULTY_ADMIN,
    ADMIN
}