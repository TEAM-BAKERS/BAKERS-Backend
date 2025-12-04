package com.example.bakersbackend.domain.match.dto;

import com.example.bakersbackend.domain.match.entity.CrewMatch;

public record MatchDetailResponse(
        MatchResponse match,
        CrewMatchDetail myCrewDetail,
        CrewMatchDetail opponentCrewDetail
) {
    public static MatchDetailResponse of(
            CrewMatch match,
            CrewMatchDetail myCrewDetail,
            CrewMatchDetail opponentCrewDetail
    ) {
        return new MatchDetailResponse(
                MatchResponse.from(match),
                myCrewDetail,
                opponentCrewDetail
        );
    }
}
