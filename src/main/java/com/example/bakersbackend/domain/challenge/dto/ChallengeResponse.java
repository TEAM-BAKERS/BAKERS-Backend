package com.example.bakersbackend.domain.challenge.dto;

import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;

import java.time.LocalDateTime;

public record ChallengeResponse(
        Long challengeId,
        String title,
        String description,
        Integer goalValue,
        Integer currentAccumulatedDistance,
        ChallengeStatus status,
        Double progressPercentage,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static ChallengeResponse from(CrewChallenge challenge) {
        double progressPercentage = challenge.getGoalValue() > 0
                ? (double) challenge.getCurrentAccumulatedDistance() / challenge.getGoalValue() * 100
                : 0.0;

        return new ChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getGoalValue(),
                challenge.getCurrentAccumulatedDistance(),
                challenge.getStatus(),
                Math.min(progressPercentage, 100.0),
                challenge.getStartAt(),
                challenge.getEndAt()
        );
    }
}