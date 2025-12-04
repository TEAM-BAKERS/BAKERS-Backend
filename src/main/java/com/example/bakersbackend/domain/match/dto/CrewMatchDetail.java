package com.example.bakersbackend.domain.match.dto;

import com.example.bakersbackend.domain.crew.entity.Crew;

import java.util.List;

public record CrewMatchDetail(
        Long crewId,
        String crewName,
        Integer totalDistance,
        List<MemberContribution> memberContributions
) {
    public static CrewMatchDetail of(
            Crew crew,
            Integer totalDistance,
            List<MemberContribution> contributions
    ) {
        return new CrewMatchDetail(
                crew.getId(),
                crew.getName(),
                totalDistance,
                contributions
        );
    }
}
