package com.example.bakersbackend.domain.match.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.repository.CrewMatchParticipantRepository;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CrewMatchServiceTest {

    @Autowired
    private CrewMatchService crewMatchService;

    @Autowired
    private CrewMatchRepository crewMatchRepository;

    @Autowired
    private CrewMatchParticipantRepository crewMatchParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrewRepository crewRepository;

    private User testUser;
    private Crew testCrew1;
    private Crew testCrew2;

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
        crewMatchParticipantRepository.deleteAll();
        crewMatchRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@test.com")
                .passwordHash("hash")
                .nickname("테스터")
                .build());

        testCrew1 = crewRepository.save(Crew.builder()
                .name("크루A")
                .owner(testUser)
                .build());

        testCrew2 = crewRepository.save(Crew.builder()
                .name("크루B")
                .owner(testUser)
                .build());
    }

    @Test
    @DisplayName("진행 중인 매치가 없으면 업데이트하지 않고 종료한다")
    @Transactional
    void updateMatchScore_NoMatchOngoing_DoesNothing() {
        // Given: 진행 중인 매치 없음
        // When
        crewMatchService.updateMatchScore(testCrew1, 5000);

        // Then: 아무것도 생성되지 않음
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).isEmpty();
    }

    @Test
    @DisplayName("배틀 리그 점수 업데이트 시 크루의 총 거리가 증가한다")
    @Transactional
    void updateMatchScore_Success() {
        // Given: 2025-01-15 12:00:00 기준 진행 중인 매치
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("1월 배틀 리그")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When
        crewMatchService.updateMatchScore(testCrew1, 8000);

        // Then
        CrewMatchParticipant participant = crewMatchParticipantRepository
                .findByMatchAndCrew(match, testCrew1).orElseThrow();
        assertThat(participant.getTotalDistance()).isEqualTo(8000);
    }

    @Test
    @DisplayName("동일 크루가 여러 번 점수를 얻으면 누적된다")
    @Transactional
    void updateMatchScore_MultipleUpdates_Accumulates() {
        // Given
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("누적 테스트 매치")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When
        crewMatchService.updateMatchScore(testCrew1, 3000);
        crewMatchService.updateMatchScore(testCrew1, 4000);
        crewMatchService.updateMatchScore(testCrew1, 5000);

        // Then
        CrewMatchParticipant participant = crewMatchParticipantRepository
                .findByMatchAndCrew(match, testCrew1).orElseThrow();
        assertThat(participant.getTotalDistance()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("여러 크루가 동시에 참가하면 각각 점수가 관리된다")
    @Transactional
    void updateMatchScore_MultipleCrews_MaintainsSeperateScores() {
        // Given
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("다중 크루 매치")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        // When
        crewMatchService.updateMatchScore(testCrew1, 7000);
        crewMatchService.updateMatchScore(testCrew2, 9000);
        crewMatchService.updateMatchScore(testCrew1, 3000); // 크루A 추가 점수

        // Then
        CrewMatchParticipant participant1 = crewMatchParticipantRepository
                .findByMatchAndCrew(match, testCrew1).orElseThrow();
        assertThat(participant1.getTotalDistance()).isEqualTo(10_000); // 7km + 3km

        CrewMatchParticipant participant2 = crewMatchParticipantRepository
                .findByMatchAndCrew(match, testCrew2).orElseThrow();
        assertThat(participant2.getTotalDistance()).isEqualTo(9000);

        List<CrewMatchParticipant> allParticipants = crewMatchParticipantRepository.findAll();
        assertThat(allParticipants).hasSize(2);
    }

    @Test
    @DisplayName("매치 기간 외의 시간에는 업데이트되지 않는다")
    @Transactional
    void updateMatchScore_OutsideMatchPeriod_DoesNotUpdate() {
        // Given: 2025-02-01 ~ 2025-02-28 매치 (현재 시간: 2025-01-15)
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("2월 매치")
                .startAt(LocalDateTime.of(2025, 2, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 2, 28, 23, 59))
                .build());

        // When: 현재 시간(2025-01-15)에 점수 업데이트 시도
        crewMatchService.updateMatchScore(testCrew1, 5000);

        // Then: 참가자가 생성되지 않음 (기간 외)
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).isEmpty();
    }

    @Test
    @DisplayName("여러 매치가 있어도 현재 진행 중인 매치만 업데이트된다")
    @Transactional
    void updateMatchScore_MultipleMatches_OnlyOngoingMatchUpdated() {
        // Given
        CrewMatch pastMatch = crewMatchRepository.save(CrewMatch.builder()
                .title("과거 매치")
                .startAt(LocalDateTime.of(2024, 12, 1, 0, 0))
                .endAt(LocalDateTime.of(2024, 12, 31, 23, 59))
                .build());

        CrewMatch ongoingMatch = crewMatchRepository.save(CrewMatch.builder()
                .title("진행 중 매치")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        CrewMatch futureMatch = crewMatchRepository.save(CrewMatch.builder()
                .title("미래 매치")
                .startAt(LocalDateTime.of(2025, 2, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 2, 28, 23, 59))
                .build());

        // When: 현재 시간(2025-01-15)에 점수 업데이트
        crewMatchService.updateMatchScore(testCrew1, 6000);

        // Then: 진행 중인 매치에만 참가자가 생성됨
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).hasSize(1);

        CrewMatchParticipant participant = participants.get(0);
        assertThat(participant.getMatch().getId()).isEqualTo(ongoingMatch.getId());
        assertThat(participant.getTotalDistance()).isEqualTo(6000);
    }
}