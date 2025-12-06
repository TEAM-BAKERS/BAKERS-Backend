package com.example.bakersbackend.domain.challenge.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.dto.AllChallengesResponse;
import com.example.bakersbackend.domain.challenge.dto.ChallengeResponse;
import com.example.bakersbackend.domain.challenge.dto.PersonalChallengeResponse;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.service.CrewChallengeService;
import com.example.bakersbackend.domain.challenge.service.PersonalChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class AllChallengesController {

    private final CrewChallengeService crewChallengeService;
    private final PersonalChallengeService personalChallengeService;

    @GetMapping
    public ResponseEntity<AllChallengesResponse> getAllChallenges(
            @AuthenticationPrincipal User user
    ) {
        // 1. 사용자가 속한 크루의 활성 챌린지 조회
        List<CrewChallenge> crewChallenges = crewChallengeService.getUserCrewActiveChallenges(user);
        List<ChallengeResponse> crewChallengeResponses = crewChallenges.stream()
                .map(ChallengeResponse::from)
                .toList();

        // 2. 개인 챌린지 조회
        List<PersonalChallengeResponse> personalChallenges = personalChallengeService.getPersonalChallenges(user);

        // 3. 통합 응답 생성
        AllChallengesResponse response = AllChallengesResponse.of(
                crewChallengeResponses,
                personalChallenges
        );

        return ResponseEntity.ok(response);
    }
}