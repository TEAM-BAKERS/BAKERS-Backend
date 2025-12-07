package com.example.bakersbackend.domain.running.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.running.dto.RunningRecordResponse;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.service.RunningRecordService;
import com.example.bakersbackend.domain.ocr.OcrService;
import com.example.bakersbackend.domain.ocr.OcrResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/runnings")
@RequiredArgsConstructor
public class RunningController {

    private final RunningRecordService runningRecordService;
    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final OcrService ocrService;

    @PostMapping
    public ResponseEntity<RunningRecordResponse> createRunning(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestPart("image") MultipartFile image
    ) {
        // 1. User 조회 (인증된 사용자)
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 현재 Crew 조회
        Long currentCrewId = user.getCurrentGroupId();
        if (currentCrewId == null) {
            throw new IllegalStateException("현재 참여 중인 크루가 없습니다. 크루 가입 후 러닝 기록을 업로드 해주세요.");
        }

        Crew crew = crewRepository.findById(currentCrewId)
                .orElseThrow(() -> new EntityNotFoundException("현재 크루를 찾을 수 없습니다."));

        // 3. OCR 로직 실행
        OcrResponse ocr = ocrService.process(image);

        // 4. 필드 타입으로 변환
        LocalDate runningDate = LocalDate.parse(ocr.date()); // "2024-11-24"
        int distanceMeters = (int) Math.round(ocr.distance() * 1000); // km -> m
        int durationSeconds = parseDurationToSeconds(ocr.duration()); // "59:37" -> 초
        Integer paceSeconds = parsePaceToSeconds(ocr.pace());         // "6'04''" -> 초

        // 시작 시각은 날짜 기준 자정
        LocalDateTime startedAt = runningDate.atStartOfDay();

        // 5. Running 엔티티 생성
        Running running = Running.builder()
                .user(user)
                .crew(crew)
                .distance(distanceMeters)
                .duration(durationSeconds)
                .avgHeartrate(null) // OCR로는 아직 심박수 안하기로함
                .pace(paceSeconds)
                .startedAt(startedAt)
                .build();

        // 6. 러닝 기록 저장 및 후처리 (챌린지, 배틀 리그 업데이트)
        Running savedRunning = runningRecordService.saveRunningWithPostProcessing(running);

        // 7. 응답 생성
        RunningRecordResponse response = RunningRecordResponse.from(savedRunning);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 파싱 함수
    private int parseDurationToSeconds(String duration) {
        if (duration == null || duration.isBlank()) {
            return 0;
        }
        String[] parts = duration.split(":");
        if (parts.length == 2) {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } else if (parts.length == 3) {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        } else {
            throw new IllegalArgumentException("지원하지 않는 duration 형식입니다: " + duration);
        }
    }

    private Integer parsePaceToSeconds(String pace) {
        // 기대 형식: "6'04''" 또는 "6'04"
        if (pace == null || pace.isBlank()) {
            return null;
        }
        String cleaned = pace
                .replace("''", "'")
                .replace("\"", "")
                .trim();

        String[] parts = cleaned.split("'");
        if (parts.length < 2) {
            throw new IllegalArgumentException("지원하지 않는 pace 형식입니다: " + pace);
        }
        int minutes = Integer.parseInt(parts[0].trim());
        int seconds = Integer.parseInt(parts[1].trim());
        return minutes * 60 + seconds;
    }
}