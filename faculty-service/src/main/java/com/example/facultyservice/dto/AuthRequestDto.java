package com.example.facultyservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO representing the payload for an authentication request. Clients send a username
 * and password which are used to authenticate and issue a JWT.
 */
public class AuthRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    public AuthRequestDto() {}

    public AuthRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
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
}