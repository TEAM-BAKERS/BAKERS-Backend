package com.example.bakersbackend.domain.challenge.dto;

import com.example.bakersbackend.domain.challenge.entity.ChallengeType;

import java.time.LocalDateTime;

public record PersonalChallengeResponse(
        String title,
        String description,
        ChallengeType type,
        Integer goalValue,
        Integer currentValue,
        Integer progressRate,
        Long daysRemaining,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}