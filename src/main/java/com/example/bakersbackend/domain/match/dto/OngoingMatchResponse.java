package com.example.bakersbackend.domain.match.dto;

import java.util.List;

public record OngoingMatchResponse(
        MatchResponse match,
        List<LeaderboardEntry> leaderboard
) {
}