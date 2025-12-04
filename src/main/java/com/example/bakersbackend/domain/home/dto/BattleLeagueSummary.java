package com.example.bakersbackend.domain.home.dto;

public record BattleLeagueSummary(
        String myCrewName,
        String opponentCrewName,
        Integer myCrewDistance,
        Integer opponentCrewDistance
) {
}
