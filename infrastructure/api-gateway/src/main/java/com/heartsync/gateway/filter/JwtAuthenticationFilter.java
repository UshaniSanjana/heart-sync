package com.heartsync.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GlobalFilter runs on EVERY request through the gateway.
 *
 * Flow:
 *   1. Is this an open endpoint (login, register)? → pass through
 *   2. Does the request have an Authorization: Bearer <token> header? → if not, 401
 *   3. Is the JWT signature valid and not expired? → if not, 401
 *   4. Add X-User-Id, X-User-Role, X-User-Email headers to the forwarded request
 *      so downstream services know who the caller is without re-validating the token.
 *
 * Why GlobalFilter instead of per-route GatewayFilter?
 *   Security should be opt-OUT (open endpoints listed below), not opt-IN.
 *   A new route is secured by default unless explicitly added to OPEN_ENDPOINTS.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Endpoints that do NOT require a JWT token.
     * Any request whose path starts with one of these strings is passed through.
     */
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator"          // health checks from Docker / load balancers
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Step 1: Skip JWT check for open endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Step 2: Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return unauthorized(exchange);
        }

        // Step 3: Validate the JWT token
        String token = authHeader.substring(7); // strip "Bearer "
        Claims claims;
        try {
            claims = validateToken(token);
        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange);
        }

        // Step 4: Inject user context headers for downstream services.
        // Services trust these headers because only the gateway can set them
        // (clients connect through the gateway, never directly to services).
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id",    claims.getSubject())
                .header("X-User-Role",  claims.get("role",  String.class))
                .header("X-User-Email", claims.get("email", String.class))
                // Strip the original Authorization header so services
                // don't have to deal with JWT at all
                .headers(h -> h.remove("Authorization"))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // Throws JwtException if signature is wrong or token is expired.
        // The caller catches this and returns 401.
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Ordering: -1 means this filter runs before all route filters.
     * This ensures auth is checked before any routing happens.
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
