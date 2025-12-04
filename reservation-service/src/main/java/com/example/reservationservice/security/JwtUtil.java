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
@Component
public class JwtUtil {
    private final SecretKey secretKey;
    public JwtUtil(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
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
    public Claims parseToken(String token) {
        Jws<Claims> jws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
        return jws.getBody();
    }
    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }
    public String getRole(String token) {
        return (String) parseToken(token).get("role", String.class);
    }
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}