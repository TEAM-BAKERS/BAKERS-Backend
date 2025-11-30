package com.example.bakersbackend.domain.challenge.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.dto.ChallengeResponse;
import com.example.bakersbackend.domain.challenge.dto.CreateChallengeRequest;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.service.CrewChallengeService;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crews/{crewId}/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final CrewChallengeService crewChallengeService;
    private final CrewRepository crewRepository;

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(
            @PathVariable Long crewId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateChallengeRequest request
    ) {
        // 1. Crew 조회
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new EntityNotFoundException("크루를 찾을 수 없습니다."));

        // 2. 챌린지 생성
        CrewChallenge challenge = crewChallengeService.createChallenge(
                crew,
                request.title(),
                request.description(),
                request.goalDistance(),
                request.endDate()
        );

        // 3. 응답 생성
        ChallengeResponse response = ChallengeResponse.from(challenge);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getChallenges(
            @PathVariable Long crewId,
            @AuthenticationPrincipal User user
    ) {
        // 1. Crew 조회
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new EntityNotFoundException("크루를 찾을 수 없습니다."));

        // 2. 챌린지 목록 조회
        List<CrewChallenge> challenges = crewChallengeService.getAllChallenges(crew);

        // 3. 응답 생성
        List<ChallengeResponse> response = challenges.stream()
                .map(ChallengeResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}