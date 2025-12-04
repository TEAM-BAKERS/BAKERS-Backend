package com.example.bakersbackend.domain.crew.dto;

public record TodayRunnerData(
        Long userId,
        String username,
        String profileImageUrl    // 필요 없으면 나중에 제거하면 됨
) {}
