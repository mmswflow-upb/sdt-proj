package com.example.facultyservice.service;
import com.example.facultyservice.entity.User;
import com.example.facultyservice.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private static final long TOKEN_TTL_MILLIS = 24L * 60 * 60 * 1000;
    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    public String authenticate(String username, String rawPassword) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        String userId = user.getId().toString();
        String role = user.getRole().name();
        return jwtUtil.generateToken(userId, role, TOKEN_TTL_MILLIS);
    }
}