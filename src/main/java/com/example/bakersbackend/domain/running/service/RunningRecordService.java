package com.example.bakersbackend.domain.running.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.service.CrewChallengeService;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.match.service.CrewMatchService;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningRecordService {

    private final RunningRepository runningRepository;
    private final CrewChallengeService crewChallengeService;
    private final CrewMatchService crewMatchService;

    /**
     * 러닝 기록을 저장하고 후처리 로직을 실행합니다.
     * 1. 러닝 기록 저장
     * 2. 챌린지 진행률 갱신 (협동)
     * 3. 배틀 리그 점수 갱신 (경쟁)
     */
    @Transactional
    public Running saveRunningWithPostProcessing(Running running) {
        // 1. 러닝 기록 저장
        Running savedRunning = runningRepository.save(running);
        log.info("러닝 기록 저장 완료: userId={}, crewId={}, distance={}m",
                savedRunning.getUser().getId(),
                savedRunning.getCrew().getId(),
                savedRunning.getDistance());

        User user = savedRunning.getUser();
        Crew crew = savedRunning.getCrew();
        Integer distance = savedRunning.getDistance();

        // 2. 챌린지 진행률 갱신 (협동)
        try {
            crewChallengeService.updateChallengeProgress(crew, user, distance);
        } catch (Exception e) {
            log.error("챌린지 진행률 업데이트 실패: userId={}, crewId={}, distance={}m",
                    user.getId(), crew.getId(), distance, e);
            // 챌린지 업데이트 실패 시에도 러닝 기록은 유지
        }

        // 3. 배틀 리그 점수 갱신 (경쟁)
        try {
            crewMatchService.updateMatchScore(crew, distance);
        } catch (Exception e) {
            log.error("배틀 리그 점수 업데이트 실패: crewId={}, distance={}m",
                    crew.getId(), distance, e);
            // 배틀 리그 업데이트 실패 시에도 러닝 기록은 유지
        }

        return savedRunning;
    }
}