package com.heartsync.iam.controller;

import com.heartsync.iam.dto.AuthResponse;
import com.heartsync.iam.dto.LoginRequest;
import com.heartsync.iam.dto.RegisterRequest;
import com.heartsync.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new clinician.
     * POST /api/auth/register
     * Body: { "email": "...", "password": "...", "fullName": "...", "role": "DOCTOR" }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Login and receive a JWT.
     * POST /api/auth/login
     * Body: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Simple endpoint to verify the gateway is forwarding X-User-* headers correctly.
     * GET /api/auth/me  (requires valid JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader("X-User-Id")    String userId,
            @RequestHeader("X-User-Role")  String role,
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(java.util.Map.of(
                "userId", userId,
                "role",   role,
                "email",  email
        ));
    }
}
