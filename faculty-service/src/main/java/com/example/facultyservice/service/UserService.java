package com.example.facultyservice.service;
import com.example.facultyservice.entity.Role;
import com.example.facultyservice.entity.User;
import com.example.facultyservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public User register(String username, String rawPassword, String role, String facultyId) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("User with username " + username + " already exists");
        }
        Role assignedRole;
        if (role == null || role.isBlank()) {
            assignedRole = Role.STUDENT;
        } else {
            try {
                assignedRole = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }
        String encoded = passwordEncoder.encode(rawPassword);
        User user = new User(username, encoded, assignedRole, facultyId);
        return userRepository.save(user);
    }
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}