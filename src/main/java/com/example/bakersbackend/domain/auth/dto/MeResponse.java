package com.example.bakersbackend.domain.auth.dto;

public record MeResponse(
        Long id,
        String email,
        String nickname
) {}
