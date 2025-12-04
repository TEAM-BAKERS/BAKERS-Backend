package com.example.bakersbackend.domain.crew.dto;

import com.example.bakersbackend.domain.crew.entity.Crew;

public record CrewSearchResponse(
        Long id,
        String name
) {
    public static CrewSearchResponse from(Crew crew) {
        return new CrewSearchResponse(
                crew.getId(),
                crew.getName()
        );
    }
}

