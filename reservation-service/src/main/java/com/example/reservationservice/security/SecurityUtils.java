package com.example.reservationservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * Utility methods to extract common information from the Spring Security context. This service
 * does not depend on the specific user details implementation and simply reads the principal
 * and authorities from the current authentication.
 */
public final class SecurityUtils {
    private SecurityUtils() {
    }

    /**
     * Returns the user identifier stored as the principal on the current authentication or null if none.
     */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal != null ? principal.toString() : null;
    }

    /**
     * Returns the first authority of the current authentication or null if none. For this project we
     * assume a single role per user encoded as a GrantedAuthority.
     */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream().findFirst().map(GrantedAuthority::getAuthority).orElse(null);
    }
}