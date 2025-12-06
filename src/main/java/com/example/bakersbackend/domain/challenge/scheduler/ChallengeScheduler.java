package com.example.bakersbackend.domain.challenge.scheduler;

import com.example.bakersbackend.domain.challenge.service.CrewChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

    private final CrewChallengeService crewChallengeService;

    /**
     * 매일 자정(00:00)에 만료된 챌린지를 FAILED로 처리합니다.
     * - cron: "초 분 시 일 월 요일"
     * - "0 0 0 * * *" = 매일 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredChallenges() {
        log.info("=== 만료된 챌린지 일괄 처리 시작 ===");

        try {
            int failedCount = crewChallengeService.markExpiredChallengesAsFailed();

            if (failedCount == 0) {
                log.info("처리할 만료된 챌린지가 없습니다.");
            }

            log.info("=== 만료된 챌린지 일괄 처리 완료 (처리 건수: {}) ===", failedCount);
        } catch (Exception e) {
            log.error("만료된 챌린지 처리 중 오류 발생", e);
        }
    }
}