package com.example.facultyservice.service;

import com.example.facultyservice.entity.Role;
import com.example.facultyservice.entity.User;
import com.example.facultyservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private String username;
    private String password;
    private String encodedPassword;
    private String facultyId;

    @BeforeEach
    void setUp() {
        username = "john.doe";
        password = "password123";
        encodedPassword = "$2a$10$encodedHash";
        facultyId = "FAC-001";
    }

    @Test
    void register_Success_WithDefaultStudentRole() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        
        User savedUser = new User(username, encodedPassword, Role.STUDENT, facultyId);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(username, password, null, facultyId);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(Role.STUDENT, result.getRole());
        assertEquals(facultyId, result.getFacultyId());
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_Success_WithAdminRole() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        
        User savedUser = new User(username, encodedPassword, Role.ADMIN, facultyId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(username, password, "ADMIN", facultyId);

        // Assert
        assertEquals(Role.ADMIN, result.getRole());
    }

    @Test
    void register_Success_WithFacultyRole() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        
        User savedUser = new User(username, encodedPassword, Role.FACULTY_ADMIN, facultyId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(username, password, "FACULTY_ADMIN", facultyId);

        // Assert
        assertEquals(Role.FACULTY_ADMIN, result.getRole());
    }

    @Test
    void register_Success_WithBlankRole_DefaultsToStudent() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        
        User savedUser = new User(username, encodedPassword, Role.STUDENT, facultyId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(username, password, "  ", facultyId);

        // Assert
        assertEquals(Role.STUDENT, result.getRole());
    }

    @Test
    void register_ThrowsException_WhenUsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userService.register(username, password, "STUDENT", facultyId));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_ThrowsException_WhenInvalidRole() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.register(username, password, "INVALID_ROLE", facultyId));
        
        assertTrue(exception.getMessage().contains("Invalid role"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_ReturnsUser_WhenUserExists() {
        // Arrange
        User user = new User(username, encodedPassword, Role.STUDENT, facultyId);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByUsername_ReturnsEmpty_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername(username);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void register_EncodesPasswordCorrectly() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        
        User savedUser = new User(username, encodedPassword, Role.STUDENT, facultyId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.register(username, password, "STUDENT", facultyId);

        // Assert
        assertNotEquals(password, result.getPassword());
        assertEquals(encodedPassword, result.getPassword());
        verify(passwordEncoder).encode(password);
    }
}
