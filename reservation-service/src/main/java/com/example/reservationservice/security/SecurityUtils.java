package com.example.reservationservice.security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collection;
public final class SecurityUtils {
    private SecurityUtils() {
    }
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal != null ? principal.toString() : null;
    }
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream().findFirst().map(GrantedAuthority::getAuthority).orElse(null);
    }
}