package com.example.bakersbackend.domain.match.dto;

import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;

public record LeaderboardEntry(
        Integer rank,
        Long crewId,
        String crewName,
        Integer totalDistance
) {
    public static LeaderboardEntry from(CrewMatchParticipant participant, Integer rank) {
        return new LeaderboardEntry(
                rank,
                participant.getCrew().getId(),
                participant.getCrew().getName(),
                participant.getTotalDistance()
        );
    }
}