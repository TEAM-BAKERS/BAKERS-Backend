package com.example.bakersbackend.domain.challenge.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.entity.CrewChallengeProgress;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeProgressRepository;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CrewChallengeServiceTest {

    @Autowired
    private CrewChallengeService crewChallengeService;

    @Autowired
    private CrewChallengeRepository crewChallengeRepository;

    @Autowired
    private CrewChallengeProgressRepository crewChallengeProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrewRepository crewRepository;

    private User testUser;
    private Crew testCrew;

    @BeforeEach
    void setUp() {
        crewChallengeProgressRepository.deleteAll();
        crewChallengeRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .nickname("테스터")
                .build());

        testCrew = crewRepository.save(Crew.builder()
                .name("테스트 크루")
                .owner(testUser)
                .build());
    }

    @Test
    @DisplayName("활성 챌린지가 없으면 업데이트하지 않고 종료한다")
    @Transactional
    void updateChallengeProgress_NoChallengeActive_DoesNothing() {
        // Given: 활성 챌린지 없음
        // When
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 5000);

        // Then: 아무것도 생성되지 않음
        List<CrewChallengeProgress> progressList = crewChallengeProgressRepository.findAll();
        assertThat(progressList).isEmpty();
    }

    @Test
    @DisplayName("챌린지 진행률 업데이트 시 크루 전체 거리와 개인 기여도가 증가한다")
    @Transactional
    void updateChallengeProgress_Success() {
        // Given
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("테스트 챌린지")
                .goalValue(100_000)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 5000);

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(5000);

        CrewChallengeProgress progress = crewChallengeProgressRepository
                .findByChallengeAndUser(challenge, testUser).orElseThrow();
        assertThat(progress.getContributedDistance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("동일 유저가 여러 번 기여하면 누적된다")
    @Transactional
    void updateChallengeProgress_MultipleContributions_Accumulates() {
        // Given
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("누적 테스트")
                .goalValue(50_000)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 3000);
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 4000);
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 5000);

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(12_000);

        CrewChallengeProgress progress = crewChallengeProgressRepository
                .findByChallengeAndUser(challenge, testUser).orElseThrow();
        assertThat(progress.getContributedDistance()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("목표 거리 달성 시 챌린지 상태가 SUCCESS로 변경된다")
    @Transactional
    void updateChallengeProgress_GoalReached_StatusChangedToSuccess() {
        // Given: 목표 10km
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("10km 달성")
                .goalValue(10_000)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When: 정확히 10km 기여
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 10_000);

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("목표 초과 달성 시에도 SUCCESS로 변경된다")
    @Transactional
    void updateChallengeProgress_ExceedGoal_StatusChangedToSuccess() {
        // Given: 목표 10km
        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("10km 달성")
                .goalValue(10_000)
                .currentAccumulatedDistance(8000) // 이미 8km 달성
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When: 3km 추가 (총 11km)
        crewChallengeService.updateChallengeProgress(testCrew, testUser, 3000);

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getStatus()).isEqualTo(ChallengeStatus.SUCCESS);
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(11_000);
    }

    @Test
    @DisplayName("여러 유저가 동시에 기여해도 데이터 무결성이 보장된다")
    @Transactional
    void updateChallengeProgress_MultipleUsers_DataIntegrityMaintained() {
        // Given
        User user1 = userRepository.save(User.builder()
                .email("user1@test.com")
                .passwordHash("hash1")
                .nickname("유저1")
                .build());

        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash2")
                .nickname("유저2")
                .build());

        User user3 = userRepository.save(User.builder()
                .email("user3@test.com")
                .passwordHash("hash3")
                .nickname("유저3")
                .build());

        CrewChallenge challenge = crewChallengeRepository.save(CrewChallenge.builder()
                .crew(testCrew)
                .title("다중 유저 테스트")
                .goalValue(30_000)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When: 세 명이 각각 5km씩 기여
        crewChallengeService.updateChallengeProgress(testCrew, user1, 5000);
        crewChallengeService.updateChallengeProgress(testCrew, user2, 5000);
        crewChallengeService.updateChallengeProgress(testCrew, user3, 5000);

        // Then
        CrewChallenge updatedChallenge = crewChallengeRepository.findById(challenge.getId()).orElseThrow();
        assertThat(updatedChallenge.getCurrentAccumulatedDistance()).isEqualTo(15_000);

        List<CrewChallengeProgress> progressList = crewChallengeProgressRepository.findAll();
        assertThat(progressList).hasSize(3);
        assertThat(progressList).allMatch(p -> p.getContributedDistance().equals(5000));
    }
}