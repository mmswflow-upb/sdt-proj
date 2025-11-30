package com.example.facultyservice.controller;

import com.example.facultyservice.dto.AuthRequestDto;
import com.example.facultyservice.dto.AuthResponseDto;
import com.example.facultyservice.dto.RegisterRequestDto;
import com.example.facultyservice.entity.User;
import com.example.facultyservice.service.AuthService;
import com.example.facultyservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling authentication and user registration endpoints. The /auth endpoints
 * are publicly accessible and do not require a JWT. Registration assigns a default role of
 * STUDENT when none is provided. Login returns a signed JWT on success.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * Registers a new user. A missing or empty role will default to STUDENT. Returns 201 CREATED
     * on success along with a simple message. If the username already exists a 409 CONFLICT
     * response is returned.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword(), request.getRole(), request.getFacultyId());
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered with id " + user.getId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Authenticates a user using username and password. Returns a JWT if the credentials are
     * valid. If authentication fails a 401 response is returned with a generic error message
     * to avoid revealing whether the username exists.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        try {
            String token = authService.authenticate(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(new AuthResponseDto(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}