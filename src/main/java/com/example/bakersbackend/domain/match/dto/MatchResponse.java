package com.example.bakersbackend.domain.match.dto;

import com.example.bakersbackend.domain.match.entity.CrewMatch;

import java.time.LocalDateTime;

public record MatchResponse(
        Long matchId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static MatchResponse from(CrewMatch match) {
        return new MatchResponse(
                match.getId(),
                match.getTitle(),
                match.getDescription(),
                match.getStartAt(),
                match.getEndAt()
        );
    }
}