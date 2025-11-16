package com.example.bakersbackend.domain.auth.dto;

public record SignUpResponse(
        Long id,
        String email,
        String nickname
) {
}
