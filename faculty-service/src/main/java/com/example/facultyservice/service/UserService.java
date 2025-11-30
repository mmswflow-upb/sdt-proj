package com.example.facultyservice.service;

import com.example.facultyservice.entity.Role;
import com.example.facultyservice.entity.User;
import com.example.facultyservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service encapsulating user management logic. Provides methods to register a new user
 * and to look up users by username. Passwords are hashed using the configured PasswordEncoder.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user. If a user with the same username already exists an IllegalStateException
     * will be thrown. If the role is null or empty, the default role STUDENT is used. Passwords
     * are hashed before being stored.
     *
     * @param username unique username
     * @param rawPassword plaintext password
     * @param role optional role name
     * @param facultyId faculty id to associate the user with
     * @return the created user entity
     */
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

    /**
     * Finds a user by username.
     *
     * @param username the username to look up
     * @return an Optional containing the found user or empty if none exists
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}