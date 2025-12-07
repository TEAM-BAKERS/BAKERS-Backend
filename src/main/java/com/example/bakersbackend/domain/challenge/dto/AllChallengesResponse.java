package com.example.bakersbackend.domain.challenge.dto;

import java.util.List;

public record AllChallengesResponse(
        List<ChallengeResponse> crewChallenges,
        List<PersonalChallengeResponse> personalChallenges
) {
    public static AllChallengesResponse of(
            List<ChallengeResponse> crewChallenges,
            List<PersonalChallengeResponse> personalChallenges
    ) {
        return new AllChallengesResponse(crewChallenges, personalChallenges);
    }
}