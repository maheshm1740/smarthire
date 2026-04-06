package com.smarthire.candidate_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.public-key}")
    private String publicKeyBase64;

    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        byte[] pubBytes = Base64.getMimeDecoder().decode(publicKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        publicKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
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
}
