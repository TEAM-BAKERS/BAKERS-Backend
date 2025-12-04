package com.example.bakersbackend.domain.crew.dto;

import java.util.List;

public record CrewSummaryData(
        Long id,
        String name,
        String intro,
        String imgUrl,
        CrewStatsData stats,
        CrewChallengeData teamChallenge,
        List<TodayRunnerData> todayMembers,
        CrewInfoData info
) {}
