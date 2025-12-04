package com.example.bakersbackend.domain.home.dto;

import java.util.List;

public record HomeResponse(
        BattleLeagueSummary battleLeague,
        TodayRunningRecord todayRunning,
        List<RecentCrewActivity> recentActivities
) {
}
