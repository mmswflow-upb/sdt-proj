package com.example.reservationservice.security;

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
 * Utility class responsible for generating and verifying JSON Web Tokens. The secret key is
 * injected from configuration and is shared across all microservices. Tokens carry a subject
 * (the user identifier) and a custom claim named "role" storing the user's role. Tokens
 * expire after a configurable duration.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a JWT containing the given subject and claims. The token will expire after the
     * specified TTL in milliseconds.
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
     * Parses the token and returns the contained claims. Throws runtime exceptions on validation errors.
     */
    public Claims parseToken(String token) {
        // Use the older parser() API for broader compatibility with jjwt versions
        Jws<Claims> jws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
        return jws.getBody();
    }

    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return (String) parseToken(token).get("role", String.class);
    }

    /**
     * Validates that the token is well formed and not expired. Returns true if the token is valid.
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