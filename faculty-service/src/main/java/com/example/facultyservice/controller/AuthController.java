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
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword(), request.getRole(), request.getFacultyId());
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered with id " + user.getId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
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