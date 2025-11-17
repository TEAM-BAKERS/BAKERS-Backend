package com.example.bakersbackend.global.jwt;

import com.example.bakersbackend.domain.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        this.accessTokenValiditySeconds = jwtProperties.getAccessTokenValiditySeconds();
        this.refreshTokenValiditySeconds = jwtProperties.getRefreshTokenValiditySeconds();
    }

    public String generateAccessToken(User user ) {
        return buildToken(user, accessTokenValiditySeconds);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenValiditySeconds);
    }

    private String buildToken(User user, long validitySeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validitySeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
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
}
