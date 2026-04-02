package com.smarthire.auth_service.service;

import com.smarthire.auth_service.dto.request.LoginRequest;
import com.smarthire.auth_service.dto.request.RefreshRequest;
import com.smarthire.auth_service.dto.request.RegisterRequest;
import com.smarthire.auth_service.dto.response.AuthResponse;
import com.smarthire.auth_service.dto.response.ValidateResponse;
import com.smarthire.auth_service.entity.User;
import com.smarthire.auth_service.exception.AuthException;
import com.smarthire.auth_service.kafka.AuthEventProducer;
import com.smarthire.auth_service.kafka.UserRegisteredEvent;
import com.smarthire.auth_service.repository.UserRepository;
import com.smarthire.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthEventProducer eventProducer;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final String REFRESH_KEY_PREFIX   = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AuthException("Email already registered");
        }

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .role(req.getRole() != null ? req.getRole() : User.Role.CANDIDATE)
                .provider("local")
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} ({})", user.getEmail(), user.getRole());

        eventProducer.publishUserRegistered(UserRegisteredEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .provider("local")
                .registeredAt(Instant.now())
                .build());

        return buildTokenResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (user.getPasswordHash() == null) {
            throw new AuthException("This account uses Google login. Please sign in with Google.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }

        return buildTokenResponse(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        String refreshToken = req.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        if (!"refresh".equals(jwtService.extractTokenType(refreshToken))) {
            throw new AuthException("Not a refresh token");
        }

        var userId = jwtService.extractUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (!refreshToken.equals(stored)) {
            throw new AuthException("Refresh token has been rotated or revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        return buildTokenResponse(user);
    }

    public void logout(String accessToken, String refreshToken) {
        if (jwtService.isTokenValid(accessToken)) {
            Claims claims = jwtService.validateToken(accessToken);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_KEY_PREFIX + accessToken,
                        "revoked",
                        Duration.ofMillis(ttl)
                );
            }
        }

        if (refreshToken != null && jwtService.isTokenValid(refreshToken)) {
            var userId = jwtService.extractUserId(refreshToken);
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        }
    }

    public ValidateResponse validate(String token) {
        if (!jwtService.isTokenValid(token)) {
            return ValidateResponse.builder().valid(false).build();
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            return ValidateResponse.builder().valid(false).build();
        }

        try {
            Claims claims = jwtService.validateToken(token);
            return ValidateResponse.builder()
                    .valid(true)
                    .userId(claims.getSubject())
                    .email((String) claims.get("email"))
                    .role((String) claims.get("role"))
                    .build();
        } catch (Exception e) {
            return ValidateResponse.builder().valid(false).build();
        }
    }

    @Transactional
    public User upsertOAuth2User(String provider, String providerId,
                                 String email, String firstName, String lastName) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(email)) {
                        User existing = userRepository.findByEmail(email).orElseThrow();
                        existing.setProvider(provider);
                        existing.setProviderId(providerId);
                        return userRepository.save(existing);
                    }

                    User user = User.builder()
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .role(User.Role.CANDIDATE)
                            .provider(provider)
                            .providerId(providerId)
                            .build();
                    User saved = userRepository.save(user);

                    eventProducer.publishUserRegistered(UserRegisteredEvent.builder()
                            .userId(saved.getId())
                            .email(saved.getEmail())
                            .firstName(saved.getFirstName())
                            .lastName(saved.getLastName())
                            .role(saved.getRole().name())
                            .provider(provider)
                            .registeredAt(Instant.now())
                            .build());

                    return saved;
                });
    }

    public AuthResponse buildTokenResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + user.getId(),
                refreshToken,
                Duration.ofMillis(jwtService.getRefreshExpirationMs())
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}