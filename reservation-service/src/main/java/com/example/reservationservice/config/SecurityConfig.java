package com.example.reservationservice.config;

import com.example.reservationservice.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
     * Provides a RestTemplate bean with an interceptor that forwards the user's JWT token
     * for service-to-service calls. This preserves the original user context and permissions
     * when calling scheduling-service. For async operations or background tasks without a
     * user context, a service token can be generated separately.
     */
    @Bean
    public RestTemplate restTemplate(com.example.reservationservice.security.JwtUtil jwtUtil) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Try to get the token from the current security context
            String token = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String) {
                token = (String) authentication.getCredentials();
            }
            
            // If no user token available (e.g., async operation), generate a service token
            if (token == null || token.isEmpty()) {
                token = jwtUtil.generateToken("internal-service", "ADMIN", 24L * 60 * 60 * 1000);
            }
            
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