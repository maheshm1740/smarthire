package com.smarthire.auth_service.security;

import com.smarthire.auth_service.entity.User;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyBase64;

    @Value("${jwt.public-key}")
    private String publicKeyBase64;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Getter
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        byte[] privBytes = Base64.getMimeDecoder().decode(privateKeyBase64);
        byte[] pubBytes  = Base64.getMimeDecoder().decode(publicKeyBase64);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        publicKey  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
    }

//    private String clean(String key) {
//        return key.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
//    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(privateKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(privateKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public String extractTokenType(String token) {
        return (String) validateToken(token).get("type");
    }

}