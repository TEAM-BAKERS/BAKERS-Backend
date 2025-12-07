package com.example.bakersbackend.domain.challenge.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.dto.PersonalChallengeResponse;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalChallengeService {

    private final RunningRepository runningRepository;

    /**
     * 개인 챌린지 목록 조회 (더미 데이터 + 실제 진행률 계산)
     * - 매월 1일~말일 자동 갱신
     */
    @Transactional(readOnly = true)
    public List<PersonalChallengeResponse> getPersonalChallenges(User user) {
        List<PersonalChallengeResponse> challenges = new ArrayList<>();

        // 챌린지 1: 이번 달 50km 달리기 (DISTANCE 타입)
        challenges.add(createDistanceChallenge(user));

        // 챌린지 2: 이번 달 12번 달리기 (STREAK 타입)
        challenges.add(createStreakChallenge(user));

        return challenges;
    }

    private PersonalChallengeResponse createDistanceChallenge(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = getMonthStartDate(now);
        LocalDateTime endAt = getMonthEndDate(now);

        String title = "이번 달 50km 달리기";
        String description = "꾸준함이 답이다! 월간 목표를 달성해보세요.";
        Integer goalValue = 50000; // 50km

        List<Running> runnings = runningRepository.findByUserAndStartedAtBetween(user, startAt, endAt);

        Integer currentValue = runnings.stream()
                .mapToInt(Running::getDistance)
                .sum();

        Integer progressRate = (int) Math.min(100.0 * currentValue / goalValue, 100);
        long daysRemaining = ChronoUnit.DAYS.between(now, endAt);

        return new PersonalChallengeResponse(
                title,
                description,
                ChallengeType.DISTANCE,
                goalValue,
                currentValue,
                progressRate,
                daysRemaining,
                startAt,
                endAt
        );
    }

    private PersonalChallengeResponse createStreakChallenge(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = getMonthStartDate(now);
        LocalDateTime endAt = getMonthEndDate(now);

        String title = "이번 달 12번 달리기";
        String description = "한 달 동안 12번 달려보세요!";
        Integer goalValue = 12; // 12회

        List<Running> runnings = runningRepository.findByUserAndStartedAtBetween(user, startAt, endAt);
        Integer currentValue = runnings.size();

        Integer progressRate = (int) Math.min(100.0 * currentValue / goalValue, 100);
        long daysRemaining = ChronoUnit.DAYS.between(now, endAt);

        return new PersonalChallengeResponse(
                title,
                description,
                ChallengeType.STREAK,
                goalValue,
                currentValue,
                progressRate,
                daysRemaining,
                startAt,
                endAt
        );
    }

    /**
     * 이번 달 1일 00:00:00 반환
     */
    private LocalDateTime getMonthStartDate(LocalDateTime now) {
        return now.with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * 이번 달 말일 23:59:59 반환
     */
    private LocalDateTime getMonthEndDate(LocalDateTime now) {
        return now.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999999999);
    }
}