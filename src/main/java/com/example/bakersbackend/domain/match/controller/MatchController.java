package com.example.bakersbackend.domain.match.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.match.dto.LeaderboardEntry;
import com.example.bakersbackend.domain.match.dto.MatchDetailResponse;
import com.example.bakersbackend.domain.match.dto.MatchResponse;
import com.example.bakersbackend.domain.match.dto.OngoingMatchResponse;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.service.CrewMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final CrewMatchService crewMatchService;

    @GetMapping("/ongoing")
    public ResponseEntity<OngoingMatchResponse> getOngoingMatch(
            @AuthenticationPrincipal User user
    ) {
        // 1. 현재 진행 중인 매치 조회
        Optional<CrewMatch> matchOpt = crewMatchService.getOngoingMatch();

        if (matchOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        CrewMatch match = matchOpt.get();

        // 2. 매치 순위 조회
        List<CrewMatchParticipant> participants = crewMatchService.getMatchLeaderboard(match);

        // 3. 응답 생성 (순위 부여)
        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardEntry> leaderboard = participants.stream()
                .map(participant -> LeaderboardEntry.from(participant, rank.getAndIncrement()))
                .toList();

        OngoingMatchResponse response = new OngoingMatchResponse(
                MatchResponse.from(match),
                leaderboard
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ongoing/detail")
    public ResponseEntity<MatchDetailResponse> getOngoingMatchDetail(
            @AuthenticationPrincipal User user
    ) {
        Optional<MatchDetailResponse> responseOpt = crewMatchService.getOngoingMatchDetail(user);

        return responseOpt
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}