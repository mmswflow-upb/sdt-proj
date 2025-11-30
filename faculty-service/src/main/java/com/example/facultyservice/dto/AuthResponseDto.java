package com.example.facultyservice.dto;

/**
 * DTO returned after a successful authentication. Contains the generated JWT token
 * which clients must include in subsequent requests via the Authorization header.
 */
public class AuthResponseDto {
    private String token;
    public AuthResponseDto() {}
    public AuthResponseDto(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}