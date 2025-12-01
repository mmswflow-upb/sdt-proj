package com.example.facultyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FacultyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacultyServiceApplication.class, args);
    }

    /**
     * Creates a default administrator account at startup if none exists. The default admin
     * credentials are username "admin" and password "admin" with role ADMIN and no specific
     * faculty. In a production system the credentials should be provided via environment variables
     * or an external secrets store.
     */
    @Bean
    public org.springframework.boot.CommandLineRunner bootstrapDefaultAdmin(com.example.facultyservice.repository.UserRepository userRepository,
                                                                             com.example.facultyservice.service.UserService userService,
                                                                             @org.springframework.beans.factory.annotation.Value("${admin.username}") String adminUsername,
                                                                             @org.springframework.beans.factory.annotation.Value("${admin.password}") String adminPassword) {
        return args -> {
            // Check if any admin exists
            boolean existsAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRole() == com.example.facultyservice.entity.Role.ADMIN);
            if (!existsAdmin) {
                try {
                    userService.register(adminUsername, adminPassword, "ADMIN", "");
                    System.out.println("Default admin user created: username=" + adminUsername);
                } catch (Exception e) {
                    // Ignore if already created concurrently
                }
            }
        };
    }
}