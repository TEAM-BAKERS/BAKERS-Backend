package com.example.bakersbackend.domain.challenge.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.challenge.dto.PersonalChallengeResponse;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PersonalChallengeServiceTest {

    @Autowired
    private PersonalChallengeService personalChallengeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RunningRepository runningRepository;

    @Autowired
    private CrewRepository crewRepository;

    private User testUser;
    private Crew testCrew;

    @BeforeEach
    void setUp() {
        runningRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .nickname("테스터")
                .build());

        testCrew = crewRepository.save(Crew.builder()
                .name("크루A")
                .owner(testUser)
                .build());
    }

    @Test
    @DisplayName("개인 챌린지 2개가 정상적으로 반환된다")
    @Transactional
    void getPersonalChallenges_ReturnsTwoChallenges() {
        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);

        // Then
        assertThat(challenges).hasSize(2);
        assertThat(challenges.get(0).type()).isEqualTo(ChallengeType.DISTANCE);
        assertThat(challenges.get(1).type()).isEqualTo(ChallengeType.STREAK);
    }

    @Test
    @DisplayName("거리 챌린지: 이번 달 러닝 거리가 정확히 합산된다")
    @Transactional
    void distanceChallenge_CalculatesCurrentMonthDistanceCorrectly() {
        // Given: 이번 달 러닝 3개 생성
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);

        createRunning(testUser, monthStart.plusDays(1), 10000); // 10km
        createRunning(testUser, monthStart.plusDays(5), 15000); // 15km
        createRunning(testUser, monthStart.plusDays(10), 8000); // 8km

        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);
        PersonalChallengeResponse distanceChallenge = challenges.stream()
                .filter(c -> c.type() == ChallengeType.DISTANCE)
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(distanceChallenge.title()).isEqualTo("이번 달 50km 달리기");
        assertThat(distanceChallenge.goalValue()).isEqualTo(50000);
        assertThat(distanceChallenge.currentValue()).isEqualTo(33000); // 10k + 15k + 8k
        assertThat(distanceChallenge.progressRate()).isEqualTo(66); // 33000/50000 * 100
    }

    @Test
    @DisplayName("거리 챌린지: 지난 달 러닝은 포함하지 않는다")
    @Transactional
    void distanceChallenge_DoesNotIncludeLastMonthRunnings() {
        // Given: 지난 달 러닝 + 이번 달 러닝
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime lastMonth = monthStart.minusDays(1);

        createRunning(testUser, lastMonth, 20000); // 지난 달 20km (제외되어야 함)
        createRunning(testUser, monthStart.plusDays(1), 10000); // 이번 달 10km

        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);
        PersonalChallengeResponse distanceChallenge = challenges.stream()
                .filter(c -> c.type() == ChallengeType.DISTANCE)
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(distanceChallenge.currentValue()).isEqualTo(10000); // 이번 달 것만
    }

    @Test
    @DisplayName("횟수 챌린지: 이번 달 러닝 횟수가 정확히 카운트된다")
    @Transactional
    void streakChallenge_CountsCurrentMonthRunningsCorrectly() {
        // Given: 이번 달 러닝 5개 생성
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);

        for (int i = 1; i <= 5; i++) {
            createRunning(testUser, monthStart.plusDays(i), 5000);
        }

        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);
        PersonalChallengeResponse streakChallenge = challenges.stream()
                .filter(c -> c.type() == ChallengeType.STREAK)
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(streakChallenge.title()).isEqualTo("이번 달 12번 달리기");
        assertThat(streakChallenge.goalValue()).isEqualTo(12);
        assertThat(streakChallenge.currentValue()).isEqualTo(5);
        assertThat(streakChallenge.progressRate()).isEqualTo(41); // 5/12 * 100
    }

    @Test
    @DisplayName("횟수 챌린지: 목표 달성 시 진행률이 100%로 제한된다")
    @Transactional
    void streakChallenge_ProgressRateCappedAt100Percent() {
        // Given: 12번 넘게 달리기
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);

        for (int i = 1; i <= 15; i++) {
            createRunning(testUser, monthStart.plusDays(i), 3000);
        }

        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);
        PersonalChallengeResponse streakChallenge = challenges.stream()
                .filter(c -> c.type() == ChallengeType.STREAK)
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(streakChallenge.currentValue()).isEqualTo(15);
        assertThat(streakChallenge.progressRate()).isEqualTo(100); // 125%이지만 100%로 제한
    }

    @Test
    @DisplayName("챌린지 기간이 이번 달 1일~말일로 설정된다")
    @Transactional
    void challenge_PeriodIsCurrentMonth() {
        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);
        PersonalChallengeResponse challenge = challenges.get(0);

        // Then
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedStart = now.with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime expectedEnd = now.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        assertThat(challenge.startAt()).isEqualToIgnoringNanos(expectedStart);
        assertThat(challenge.endAt().toLocalDate()).isEqualTo(expectedEnd.toLocalDate());
        assertThat(challenge.endAt().getHour()).isEqualTo(23);
        assertThat(challenge.endAt().getMinute()).isEqualTo(59);
    }

    @Test
    @DisplayName("러닝이 없을 때 진행률이 0이다")
    @Transactional
    void challenge_NoRunnings_ProgressIsZero() {
        // Given: 러닝 없음

        // When
        List<PersonalChallengeResponse> challenges = personalChallengeService.getPersonalChallenges(testUser);

        // Then
        assertThat(challenges).allMatch(c -> c.currentValue() == 0);
        assertThat(challenges).allMatch(c -> c.progressRate() == 0);
    }

    private void createRunning(User user, LocalDateTime startedAt, int distance) {
        runningRepository.save(Running.builder()
                .user(user)
                .crew(testCrew)
                .distance(distance)
                .duration(1800) // 30분 (초 단위)
                .pace(360) // 6분/km (초/km)
                .avgHeartrate((short) 150)
                .startedAt(startedAt)
                .build());
    }
}
