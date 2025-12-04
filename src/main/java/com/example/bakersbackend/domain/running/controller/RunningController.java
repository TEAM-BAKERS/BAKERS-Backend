package com.example.bakersbackend.domain.running.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.running.dto.RunningRecordRequest;
import com.example.bakersbackend.domain.running.dto.RunningRecordResponse;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.service.RunningRecordService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/runnings")
@RequiredArgsConstructor
public class RunningController {

    private final RunningRecordService runningRecordService;
    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final Clock clock;

    @PostMapping
    public ResponseEntity<RunningRecordResponse> createRunning(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody RunningRecordRequest request
    ) {
        // 1. User 조회 (인증된 사용자)
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. Crew 조회
        Crew crew = crewRepository.findById(request.crewId())
                .orElseThrow(() -> new EntityNotFoundException("크루를 찾을 수 없습니다."));

        // 3. Running 엔티티 생성
        Running running = Running.builder()
                .user(user)
                .crew(crew)
                .distance(request.distance())
                .duration(request.duration())
                .avgHeartrate(request.avgHeartrate())
                .pace(request.pace())
                .startedAt(LocalDateTime.now(clock))
                .build();

        // 4. 러닝 기록 저장 및 후처리 (챌린지, 배틀 리그 업데이트)
        Running savedRunning = runningRecordService.saveRunningWithPostProcessing(running);

        // 5. 응답 생성
        RunningRecordResponse response = RunningRecordResponse.from(savedRunning);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}