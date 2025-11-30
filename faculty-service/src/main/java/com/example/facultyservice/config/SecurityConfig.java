package com.example.facultyservice.config;

import com.example.facultyservice.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Security configuration for the faculty-service. Configures stateless JWT-based
 * authentication and plugs in our custom filter. All endpoints require authentication except
 * those under /auth. Method-level security is enabled so that controllers and services can
 * restrict access based on roles using {@link org.springframework.security.access.prepost.PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // permit unauthenticated access to auth endpoints and the error page
                    .requestMatchers("/auth/**", "/error").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Password encoder used for hashing user passwords. We use BCrypt which is a strong adaptive
     * hashing algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides a RestTemplate bean to be used for inter-service communication. In a production
     * system consider using WebClient and configuring timeouts, retries, etc.
     */
    /**
     * Provides a RestTemplate bean configured with a request interceptor that injects a
     * JWT for internal service-to-service calls. The token is generated using the
     * configured JwtUtil and carries a hard-coded subject of "internal-service" and
     * the ADMIN role. This allows the faculty-service to call other microservices
     * (scheduling-service and reservation-service) which secure their endpoints with
     * role-based access control. The token has a TTL of 24 hours.
     */
    @Bean
    public RestTemplate restTemplate(com.example.facultyservice.security.JwtUtil jwtUtil) {
        RestTemplate restTemplate = new RestTemplate();
        // Interceptor to add Authorization header
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Generate a token on each request. In a real system you might cache this.
            String token = jwtUtil.generateToken("internal-service", "ADMIN", 24L * 60 * 60 * 1000);
            request.getHeaders().add("Authorization", "Bearer " + token);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}