package com.smarthire.auth_service.controller;

import com.smarthire.auth_service.dto.request.LoginRequest;
import com.smarthire.auth_service.dto.request.RefreshRequest;
import com.smarthire.auth_service.dto.request.RegisterRequest;
import com.smarthire.auth_service.dto.response.AuthResponse;
import com.smarthire.auth_service.dto.response.ValidateResponse;
import com.smarthire.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshRequest request) {

        String accessToken  = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String refreshToken = request != null ? request.getRefreshToken() : null;

        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader("Authorization") String authHeader) {

        String token  = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        ValidateResponse result = authService.validate(token);
        int status = result.isValid() ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value();
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        ValidateResponse info = authService.validate(token);

        if (!info.isValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of(
                "userId", info.getUserId(),
                "email",  info.getEmail(),
                "role",   info.getRole()
        ));
    }
}