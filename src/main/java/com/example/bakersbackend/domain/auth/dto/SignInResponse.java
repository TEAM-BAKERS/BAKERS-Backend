package com.example.bakersbackend.domain.auth.dto;

public record SignInResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
