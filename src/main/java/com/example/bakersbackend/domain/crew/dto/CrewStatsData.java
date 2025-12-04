package com.example.bakersbackend.domain.crew.dto;

public record CrewStatsData(
        double totalDistanceKm,   // 누적 거리(km)
        long totalDurationHour,    // 누적 시간(시간)
        int goalAchieveRate       // 목표 달성률(%)
) {}
