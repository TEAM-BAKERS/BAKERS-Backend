package com.example.bakersbackend.global.jwt;

import com.example.bakersbackend.domain.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties properties;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user ) {
        return buildToken(user, JwtType.ACCESS);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, JwtType.REFRESH);
    }

    private String buildToken(User user, JwtType type) {
        Instant now = Instant.now();
        long validitySeconds = switch (type) {
            case ACCESS -> properties.getAccessTokenValiditySeconds();
            case REFRESH -> properties.getRefreshTokenValiditySeconds();
        };
        Instant exp = now.plusSeconds(validitySeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("tokenType", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getPayload().getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
