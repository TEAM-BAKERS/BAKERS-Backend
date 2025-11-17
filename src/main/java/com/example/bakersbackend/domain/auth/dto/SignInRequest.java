package com.example.bakersbackend.domain.auth.dto;

public record SignInRequest(
        String email,
        String password
) {
}
