package com.example.bakersbackend.domain.challenge.initializer;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChallengeDataInitializerTest {

    @Autowired
    private ChallengeDataInitializer initializer;

    @Autowired
    private CrewChallengeRepository crewChallengeRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Crew testCrew;

    @BeforeEach
    void setUp() {
        crewChallengeRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("owner@test.com")
                .passwordHash("hash")
                .nickname("크루장")
                .build());

        testCrew = crewRepository.save(Crew.builder()
                .name("크루A")
                .owner(testUser)
                .build());
    }

    @Test
    @DisplayName("크루당 1개의 챌린지가 생성된다")
    @Transactional
    void initializeChallenges_CreatesOneChallengePerCrew() throws Exception {
        // When
        initializer.run();

        // Then
        List<CrewChallenge> challenges = crewChallengeRepository.findByCrewOrderByCreatedAtDesc(testCrew);
        assertThat(challenges).hasSize(1);
    }

    @Test
    @DisplayName("연말 100km 챌린지가 올바르게 생성된다")
    @Transactional
    void initializeChallenges_CreatesYearEnd100kmChallenge() throws Exception {
        // When
        initializer.run();

        // Then
        LocalDateTime startAt = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
        Optional<CrewChallenge> challenge = crewChallengeRepository
                .findByCrewAndStartAtAndEndAt(testCrew, startAt, endAt);

        assertThat(challenge).isPresent();
        assertThat(challenge.get().getTitle()).isEqualTo("연말 100km 챌린지");
        assertThat(challenge.get().getGoalValue()).isEqualTo(100000);
        assertThat(challenge.get().getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
        assertThat(challenge.get().getCurrentAccumulatedDistance()).isEqualTo(0);
        assertThat(challenge.get().getStartAt()).isEqualTo(startAt);
        assertThat(challenge.get().getEndAt()).isEqualTo(endAt);
    }

    @Test
    @DisplayName("재실행 시 중복 생성되지 않는다 (진행률 유지)")
    @Transactional
    void initializeChallenges_DoesNotDuplicateOnRerun() throws Exception {
        // Given: 첫 실행으로 챌린지 생성
        initializer.run();

        LocalDateTime startAt = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        // 진행률을 임의로 변경
        CrewChallenge challenge = crewChallengeRepository
                .findByCrewAndStartAtAndEndAt(testCrew, startAt, endAt)
                .orElseThrow();
        challenge.addAccumulatedDistance(30000); // 30km 진행
        crewChallengeRepository.save(challenge);

        // When: 재실행
        initializer.run();

        // Then: 중복 생성되지 않고, 진행률은 유지됨
        List<CrewChallenge> challenges = crewChallengeRepository.findByCrewOrderByCreatedAtDesc(testCrew);
        assertThat(challenges).hasSize(1); // 여전히 1개

        CrewChallenge existing = crewChallengeRepository
                .findByCrewAndStartAtAndEndAt(testCrew, startAt, endAt)
                .orElseThrow();
        assertThat(existing.getCurrentAccumulatedDistance()).isEqualTo(30000); // 진행률 유지
    }

    @Test
    @DisplayName("여러 크루가 있을 때 각 크루마다 챌린지가 생성된다")
    @Transactional
    void initializeChallenges_CreatesForMultipleCrews() throws Exception {
        // Given: 크루 2개 더 추가
        User user2 = userRepository.save(User.builder()
                .email("owner2@test.com")
                .passwordHash("hash")
                .nickname("크루장2")
                .build());
        Crew crew2 = crewRepository.save(Crew.builder()
                .name("크루B")
                .owner(user2)
                .build());

        User user3 = userRepository.save(User.builder()
                .email("owner3@test.com")
                .passwordHash("hash")
                .nickname("크루장3")
                .build());
        Crew crew3 = crewRepository.save(Crew.builder()
                .name("크루C")
                .owner(user3)
                .build());

        // When
        initializer.run();

        // Then: 총 3개 크루 * 1개 챌린지 = 3개
        long totalChallenges = crewChallengeRepository.count();
        assertThat(totalChallenges).isEqualTo(3);

        // 각 크루마다 1개씩
        assertThat(crewChallengeRepository.findByCrewOrderByCreatedAtDesc(testCrew)).hasSize(1);
        assertThat(crewChallengeRepository.findByCrewOrderByCreatedAtDesc(crew2)).hasSize(1);
        assertThat(crewChallengeRepository.findByCrewOrderByCreatedAtDesc(crew3)).hasSize(1);
    }

    @Test
    @DisplayName("크루가 없을 때는 챌린지를 생성하지 않는다")
    @Transactional
    void initializeChallenges_NoCrews_DoesNotCreateChallenges() throws Exception {
        // Given: 모든 크루 삭제
        crewRepository.deleteAll();

        // When
        initializer.run();

        // Then
        long totalChallenges = crewChallengeRepository.count();
        assertThat(totalChallenges).isEqualTo(0);
    }

}
