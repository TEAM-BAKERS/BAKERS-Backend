package com.example.bakersbackend.domain.mypage.dto;

public record MypageSummaryResponse(
        String nickname,        // 이세빈
        String imageUrl,        // 프로필 이미지 URL
        String joinDate,        // "2025.09.24"
        double totalDistanceKm, // 245.8
        long runningCount,      // 42
        String averagePace      // "7'30\""
) {}
