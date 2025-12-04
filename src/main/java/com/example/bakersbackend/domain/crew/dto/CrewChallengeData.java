package com.example.bakersbackend.domain.crew.dto;

import com.example.bakersbackend.domain.challenge.entity.ChallengeType;

import java.time.LocalDateTime;

public record CrewChallengeData(
        Long id,
        ChallengeType type,
        int goalValue,            // 목표 값 (예: m, 초, 일수 등)
        int currentValue,         // 현재까지 달성 값
        int progressRate,         // 진행률(%)
        LocalDateTime startAt,
        LocalDateTime endAt
) {}
