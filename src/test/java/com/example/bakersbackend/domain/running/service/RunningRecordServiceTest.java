package com.example.bakersbackend.domain.running.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.entity.CrewChallengeProgress;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeProgressRepository;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.repository.CrewMatchParticipantRepository;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RunningRecordServiceTest {

    @Autowired
    private RunningRecordService runningRecordService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private RunningRepository runningRepository;

    @Autowired
    private CrewChallengeRepository crewChallengeRepository;

    @Autowired
    private CrewChallengeProgressRepository crewChallengeProgressRepository;

    @Autowired
    private CrewMatchRepository crewMatchRepository;

    @Autowired
    private CrewMatchParticipantRepository crewMatchParticipantRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Crew testCrew;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            // 고정된 시간: 2025-01-15 12:00:00
            LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 15, 12, 0, 0);
            return Clock.fixed(fixedTime.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (FK 제약 조건 고려)
        runningRepository.deleteAll();
        crewChallengeProgressRepository.deleteAll();
        crewChallengeRepository.deleteAll();
        crewMatchParticipantRepository.deleteAll();
        crewMatchRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 사용자 생성
        testUser1 = userRepository.save(User.builder()
                .email("user1@test.com")
                .passwordHash("hash1")
                .nickname("러너1")
                .build());

        testUser2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash2")
                .nickname("러너2")
                .build());

        testUser3 = userRepository.save(User.builder()
                .email("user3@test.com")
                .passwordHash("hash3")
                .nickname("러너3")
                .build());

        // 테스트 크루 생성
        testCrew = crewRepository.save(Crew.builder()
                .name("테스트 크루")
                .owner(testUser1)
                .intro("테스트용 크루입니다")
                .max(50)
                .build());
    }

    @Test
    @DisplayName("러닝 기록 저장 시 챌린지 진행률과 배틀 리그 점수가 정상적으로 갱신된다")
    @Transactional
    void saveRunningWithPostProcessing_Success() {
        // Given
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("100km 완주 챌린지")
                .description("크루 전체가 100km를 달려봅시다!")
                .goalDistance(100_000) // 100km
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("1월 배틀 리그")
                .description("1월 한 달간 진행되는 크루 대항전")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        Running running = Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(5000) // 5km
                .duration(1800) // 30분
                .startedAt(LocalDateTime.of(2025, 1, 15, 8, 0))
                .build();

        // When
        Running savedRunning = runningRecordService.saveRunningWithPostProcessing(running);

        // Then
        assertThat(savedRunning.getId()).isNotNull();

        // 챌린지 진행률 확인
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(5000);

        CrewChallengeProgress progress = crewChallengeProgressRepository
                .findByChallengeAndUser(challenge, testUser1).orElseThrow();
        assertThat(progress.getContributedDistance()).isEqualTo(5000);

        // 배틀 리그 점수 확인
        CrewMatchParticipant participant = crewMatchParticipantRepository
                .findByMatchAndCrew(match, testCrew).orElseThrow();
        assertThat(participant.getTotalDistance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 러닝 기록을 저장해도 데이터 무결성이 보장된다")
    void saveRunningWithPostProcessing_ConcurrentUsers_Success() throws InterruptedException {
        // Given
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("동시성 테스트 챌린지")
                .goalDistance(50_000) // 50km
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("동시성 테스트 매치")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        Integer threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 10명의 사용자가 동시에 각각 3km씩 러닝 기록
        for (int i = 0; i < threadCount; i++) {
            Integer finalI = i;
            executorService.submit(() -> {
                try {
                    // 각 스레드마다 새로운 사용자 생성
                    User user = userRepository.save(User.builder()
                            .email("concurrent" + finalI + "@test.com")
                            .passwordHash("hash")
                            .nickname("동시러너" + finalI)
                            .build());

                    Running running = Running.builder()
                            .user(user)
                            .crew(testCrew)
                            .distance(3000) // 3km
                            .duration(1200)
                            .startedAt(LocalDateTime.of(2025, 1, 15, 8, 0))
                            .build();

                    runningRecordService.saveRunningWithPostProcessing(running);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(threadCount);

        // 챌린지 전체 누적 거리 확인: 10명 * 3km = 30km
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(30_000);

        // 개인 기여도 확인: 10개의 진행률이 생성되어야 함
        List<CrewChallengeProgress> progressList = crewChallengeProgressRepository.findAll();
        assertThat(progressList).hasSize(threadCount);
        assertThat(progressList).allMatch(p -> p.getContributedDistance().equals(3000));

        // 배틀 리그 점수 확인 (락 없는 조회 사용)
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getTotalDistance()).isEqualTo(30_000);
    }

    @Test
    @DisplayName("챌린지 목표 달성 시 상태가 SUCCESS로 변경된다")
    @Transactional
    void saveRunningWithPostProcessing_ChallengeGoalReached_StatusChangedToSuccess() {
        // Given: 목표 10km 챌린지
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("10km 달성 챌린지")
                .goalDistance(10_000) // 10km
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When: 세 명이 각각 4km씩 러닝 (총 12km)
        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(4000)
                .duration(1200)
                .startedAt(LocalDateTime.now())
                .build());

        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser2)
                .crew(testCrew)
                .distance(4000)
                .duration(1200)
                .startedAt(LocalDateTime.now())
                .build());

        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser3)
                .crew(testCrew)
                .distance(4000)
                .duration(1200)
                .startedAt(LocalDateTime.now())
                .build());

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("진행 중인 챌린지가 없으면 에러 없이 러닝 기록만 저장된다")
    @Transactional
    void saveRunningWithPostProcessing_NoChallengeActive_OnlySavesRunning() {
        // Given: 진행 중인 챌린지 없음
        Running running = Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(5000)
                .duration(1800)
                .startedAt(LocalDateTime.now())
                .build();

        // When
        Running savedRunning = runningRecordService.saveRunningWithPostProcessing(running);

        // Then
        assertThat(savedRunning.getId()).isNotNull();
        assertThat(runningRepository.findById(savedRunning.getId())).isPresent();

        // 챌린지 진행률이 생성되지 않음
        List<CrewChallengeProgress> progressList = crewChallengeProgressRepository.findAll();
        assertThat(progressList).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 배틀 리그가 없으면 에러 없이 러닝 기록만 저장된다")
    @Transactional
    void saveRunningWithPostProcessing_NoMatchOngoing_OnlySavesRunning() {
        // Given: 진행 중인 매치 없음
        Running running = Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(5000)
                .duration(1800)
                .startedAt(LocalDateTime.now())
                .build();

        // When
        Running savedRunning = runningRecordService.saveRunningWithPostProcessing(running);

        // Then
        assertThat(savedRunning.getId()).isNotNull();

        // 배틀 리그 참가자가 생성되지 않음
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).isEmpty();
    }

    @Test
    @DisplayName("동일 유저가 여러 번 러닝하면 개인 기여도가 누적된다")
    @Transactional
    void saveRunningWithPostProcessing_SameUserMultipleTimes_AccumulatesContribution() {
        // Given
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("누적 테스트 챌린지")
                .goalDistance(20_000)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When: 동일 유저가 3번 러닝
        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(3000)
                .duration(900)
                .startedAt(LocalDateTime.now())
                .build());

        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(4000)
                .duration(1200)
                .startedAt(LocalDateTime.now())
                .build());

        runningRecordService.saveRunningWithPostProcessing(Running.builder()
                .user(testUser1)
                .crew(testCrew)
                .distance(5000)
                .duration(1500)
                .startedAt(LocalDateTime.now())
                .build());

        // Then
        CrewChallengeProgress progress = crewChallengeProgressRepository
                .findByChallengeAndUser(challenge, testUser1).orElseThrow();
        assertThat(progress.getContributedDistance()).isEqualTo(12_000); // 3km + 4km + 5km

        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(12_000);
    }
}