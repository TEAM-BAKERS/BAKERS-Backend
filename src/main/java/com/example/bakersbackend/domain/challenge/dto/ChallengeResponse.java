package com.example.bakersbackend.domain.challenge.dto;

import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ChallengeResponse(
        Long challengeId,
        String title,
        String description,
        ChallengeType type,
        Integer goalValue,
        Integer currentAccumulatedDistance,
        ChallengeStatus status,
        Double progressPercentage,
        Long daysRemaining,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static ChallengeResponse from(CrewChallenge challenge) {
        // 진행률 계산 로직은 실제로 동작
        double progressPercentage = challenge.getGoalValue() > 0
                ? (double) challenge.getCurrentAccumulatedDistance() / challenge.getGoalValue() * 100
                : 0.0;

        // 남은 일수 계산
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = ChronoUnit.DAYS.between(now, challenge.getEndAt());
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }

        // 더미 텍스트 데이터 사용
        String dummyTitle = "한강 야경 러닝 챌린지";
        String dummyDescription = "팀원들과 함께 한강을 달리며 목표 거리를 달성해보세요!";

        return new ChallengeResponse(
                challenge.getId(),
                dummyTitle,  // 더미 제목
                dummyDescription,  // 더미 설명
                challenge.getType(),
                challenge.getGoalValue(),
                challenge.getCurrentAccumulatedDistance(),
                challenge.getStatus(),
                Math.min(progressPercentage, 100.0),
                daysRemaining,
                challenge.getStartAt(),
                challenge.getEndAt()
        );
    }
}