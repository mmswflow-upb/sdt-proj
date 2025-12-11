package com.example.facultyservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
@EnableFeignClients
public class FacultyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacultyServiceApplication.class, args);
    }
    @Bean
    public org.springframework.boot.CommandLineRunner bootstrapDefaultAdmin(com.example.facultyservice.repository.UserRepository userRepository,
                                                                             com.example.facultyservice.service.UserService userService,
                                                                             @org.springframework.beans.factory.annotation.Value("${admin.username}") String adminUsername,
                                                                             @org.springframework.beans.factory.annotation.Value("${admin.password}") String adminPassword) {
        return args -> {
            boolean existsAdmin = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRole() == com.example.facultyservice.entity.Role.ADMIN);
            if (!existsAdmin) {
                try {
                    userService.register(adminUsername, adminPassword, "ADMIN", "");
                    System.out.println("Default admin user created: username=" + adminUsername);
                } catch (Exception e) {
                }
            }
        };
    }
}