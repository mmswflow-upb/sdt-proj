package com.example.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for validating JSON Web Tokens in the gateway-service. The secret key is
 * injected from configuration and must match the secret used by all microservices. This
 * class only validates tokens and does not generate them, as token generation is handled
 * by the faculty-service authentication endpoints.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the provided JWT and returns the contained claims. Throws runtime exceptions on
     * validation errors.
     */
    public Claims parseToken(String token) {
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
