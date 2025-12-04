package com.example.gateway.security;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;
    private static final List<String> EXCLUDED_PATHS = List.of("/api/auth/", "/actuator/", "/error");
    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);
                String authority = role != null && role.startsWith("ROLE_") ? role : "ROLE_" + role;
                UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singleton(new SimpleGrantedAuthority(authority))
                    );
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authenticationToken));
            }
        }
        return chain.filter(exchange);
    }
    @Override
    public int getOrder() {
        return -100;
    }
}