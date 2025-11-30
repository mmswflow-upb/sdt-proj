package com.example.facultyservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Utility class responsible for generating and verifying JSON Web Tokens for the faculty-service.
 * The secret key is injected from configuration and is shared across all microservices. Tokens
 * carry the user identifier as the subject and a custom claim named "role" storing the user's
 * role. Tokens expire after a configurable duration.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a JWT containing the given subject (user id) and role. The token will expire after
     * the specified TTL in milliseconds.
     *
     * @param subject user identifier to embed as the subject
     * @param role the user's role
     * @param ttlMillis time-to-live in milliseconds
     * @return a signed JWT
     */
    public String generateToken(String subject, String role, long ttlMillis) {
        long now = System.currentTimeMillis();
        Date expiry = new Date(now + ttlMillis);
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(Map.of("role", role))
                .setIssuedAt(new Date(now))
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses the provided JWT and returns the contained claims. Throws runtime exceptions on
     * validation errors.
     */
    public Claims parseToken(String token) {
        // Use a broadly-compatible parser API that works with the project's jjwt
        // dependencies. This mirrors the implementation used in the other services.
        Jws<Claims> jws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
        return jws.getBody();
        }

    /**
     * Extracts the user id (subject) from the token.
     */
    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Extracts the role from the token. The role claim is stored as a string.
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * Validates that the token is structurally correct and not expired.
     *
     * @param token the JWT to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}