package com.example.reservationservice.config;
import com.example.reservationservice.security.JwtUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
@Component
public class FeignClientInterceptor implements RequestInterceptor {
    private final JwtUtil jwtUtil;
    public FeignClientInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public void apply(RequestTemplate template) {
        String token = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            token = (String) authentication.getCredentials();
        }
        if (token == null || token.isEmpty()) {
            token = jwtUtil.generateToken("internal-service", "ADMIN", 24L * 60 * 60 * 1000);
        }
        template.header("Authorization", "Bearer " + token);
    }
}
