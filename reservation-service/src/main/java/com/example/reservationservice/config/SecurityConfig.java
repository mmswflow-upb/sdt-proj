package com.example.reservationservice.config;

import com.example.reservationservice.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Security configuration for the reservation-service. Configures stateless JWT based
 * authentication and plugs in our custom filter. All endpoints require authentication unless
 * explicitly permitted.
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
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Exposes a RestTemplate bean to be used for inter-service communication. Consider replacing with
     * WebClient in a real-world application for reactive and configurable HTTP calls.
     */
    /**
     * Provides a RestTemplate bean with an interceptor that injects a JWT for
     * service-to-service calls. This allows the reservation-service to call
     * scheduling-service endpoints that are secured with role-based access control.
     * The generated token uses a fixed subject "internal-service" with ADMIN role
     * and has a TTL of 24 hours. In a production environment tokens should be
     * cached and rotated appropriately.
     */
    @Bean
    public RestTemplate restTemplate(com.example.reservationservice.security.JwtUtil jwtUtil) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String token = jwtUtil.generateToken("internal-service", "ADMIN", 24L * 60 * 60 * 1000);
            request.getHeaders().add("Authorization", "Bearer " + token);
            return execution.execute(request, body);
        });
        return restTemplate;
    }

    /**
     * Provides a password encoder. It is not used in the reservation-service directly but is declared
     * here in case future enhancements require password hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}