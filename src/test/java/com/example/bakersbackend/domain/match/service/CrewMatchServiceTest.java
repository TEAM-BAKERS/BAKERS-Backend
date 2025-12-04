package com.example.bakersbackend.domain.match.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.entity.CrewMember;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.match.dto.MatchDetailResponse;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.repository.CrewMatchParticipantRepository;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import jakarta.persistence.EntityManager;
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
import java.util.Optional;

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

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    @Autowired
    private RunningRepository runningRepository;

    @Autowired
    private EntityManager entityManager;

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
        runningRepository.deleteAll();
        crewMemberRepository.deleteAll();
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

        // 참가자 추가
        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(match, testCrew1, 0));

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

        // 참가자 추가
        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(match, testCrew1, 0));

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

        // 참가자 추가
        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(match, testCrew1, 0));
        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(match, testCrew2, 0));

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

        // 진행 중인 매치에 참가자 추가
        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(ongoingMatch, testCrew1, 0));

        // When: 현재 시간(2025-01-15)에 점수 업데이트
        crewMatchService.updateMatchScore(testCrew1, 6000);

        // Then: 진행 중인 매치의 참가자만 업데이트됨
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).hasSize(1);

        CrewMatchParticipant participant = participants.get(0);
        assertThat(participant.getMatch().getId()).isEqualTo(ongoingMatch.getId());
        assertThat(participant.getTotalDistance()).isEqualTo(6000);
    }

    @Test
    @DisplayName("참가자가 아닌 크루가 러닝하면 점수 업데이트를 건너뛰고 러닝 기록은 보호된다")
    @Transactional
    void updateMatchScore_NonParticipant_SkipsUpdateAndProtectsRunning() {
        // Given: 매치에 testCrew1만 참가
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("1:1 매치")
                .startAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 1, 31, 23, 59))
                .build());

        crewMatchParticipantRepository.save(CrewMatchParticipant.createNew(match, testCrew1, 0));

        // When: testCrew2 (참가자 아님)가 러닝
        crewMatchService.updateMatchScore(testCrew2, 5000);

        // Then: 참가자는 여전히 1명, testCrew2의 참가자 레코드 없음
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getCrew().getId()).isEqualTo(testCrew1.getId());
    }

    @Test
    @DisplayName("1:1 매칭 생성 시 정확히 2개 크루만 참가한다")
    @Transactional
    void createOneOnOneMatch_Success() {
        // When
        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        // Then
        assertThat(match.getParticipants()).hasSize(2);
        assertThat(match.getStatus()).isEqualTo(com.example.bakersbackend.domain.match.entity.MatchStatus.ONGOING);
        assertThat(match.getWinner()).isNull();
    }

    @Test
    @DisplayName("승자 판정 시 거리가 많은 크루가 승리한다")
    @Transactional
    void determineWinner_HigherDistanceWins() {
        // Given: 테스트 시간(2025-01-15)에 진행 중인 매치
        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        crewMatchService.updateMatchScore(testCrew1, 10000);
        crewMatchService.updateMatchScore(testCrew2, 8000);

        // 영속성 컨텍스트 플러시 및 클리어
        entityManager.flush();
        entityManager.clear();

        // 점수 확인
        List<CrewMatchParticipant> participants = crewMatchParticipantRepository.findAll();
        assertThat(participants).hasSize(2);
        assertThat(participants).anyMatch(p -> p.getCrew().getId().equals(testCrew1.getId()) && p.getTotalDistance() == 10000);
        assertThat(participants).anyMatch(p -> p.getCrew().getId().equals(testCrew2.getId()) && p.getTotalDistance() == 8000);

        // When
        crewMatchService.finishMatch(match.getId());

        // Then
        CrewMatch finishedMatch = crewMatchRepository.findByIdWithParticipants(match.getId()).orElseThrow();
        System.out.println("=== 매치 결과 ===");
        System.out.println("Status: " + finishedMatch.getStatus());
        System.out.println("Winner: " + (finishedMatch.getWinner() != null ? finishedMatch.getWinner().getName() : "null"));

        assertThat(finishedMatch.getStatus()).isEqualTo(com.example.bakersbackend.domain.match.entity.MatchStatus.FINISHED);
        assertThat(finishedMatch.getWinner().getId()).isEqualTo(testCrew1.getId());
    }

    @Test
    @DisplayName("동점이면 무승부로 판정된다")
    @Transactional
    void determineWinner_Draw() {
        // Given: 테스트 시간(2025-01-15)에 진행 중인 매치
        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        crewMatchService.updateMatchScore(testCrew1, 10000);
        crewMatchService.updateMatchScore(testCrew2, 10000);

        // When
        crewMatchService.finishMatch(match.getId());

        // Then
        CrewMatch finishedMatch = crewMatchRepository.findById(match.getId()).orElseThrow();
        assertThat(finishedMatch.getStatus()).isEqualTo(com.example.bakersbackend.domain.match.entity.MatchStatus.DRAW);
        assertThat(finishedMatch.getWinner()).isNull();
    }

    @Test
    @DisplayName("진행 중인 매치가 없으면 상세 정보 조회 시 Optional.empty를 반환한다")
    @Transactional
    void getOngoingMatchDetail_NoMatch_ReturnsEmpty() {
        // Given: 진행 중인 매치 없음
        // When
        Optional<MatchDetailResponse> result = crewMatchService.getOngoingMatchDetail(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자가 매치 참가자가 아니면 Optional.empty를 반환한다")
    @Transactional
    void getOngoingMatchDetail_UserNotParticipant_ReturnsEmpty() {
        // Given: testUser가 참가하지 않은 매치
        User otherUser = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash("hash")
                .nickname("다른사용자")
                .build());

        Crew otherCrew1 = crewRepository.save(Crew.builder()
                .name("다른크루1")
                .owner(otherUser)
                .build());

        Crew otherCrew2 = crewRepository.save(Crew.builder()
                .name("다른크루2")
                .owner(otherUser)
                .build());

        crewMatchService.createOneOnOneMatch(
                "다른 매치",
                otherCrew1.getId(),
                otherCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        // When: testUser가 조회
        Optional<MatchDetailResponse> result = crewMatchService.getOngoingMatchDetail(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스: 내 크루와 상대 크루 정보를 정확히 반환한다")
    @Transactional
    void getOngoingMatchDetail_Success() {
        // Given: testUser가 testCrew1의 멤버
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash")
                .nickname("유저2")
                .build());

        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(user2)
                .status(MemberStatus.APPROVED)
                .build());

        // 매치 생성
        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        // 러닝 기록
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .startedAt(LocalDateTime.of(2025, 1, 12, 10, 0))
                .build());

        runningRepository.save(Running.builder()
                .user(user2)
                .crew(testCrew1)
                .distance(3000)
                .duration(1200)
                .startedAt(LocalDateTime.of(2025, 1, 12, 11, 0))
                .build());

        // 점수 업데이트
        crewMatchService.updateMatchScore(testCrew1, 5000);
        crewMatchService.updateMatchScore(testCrew1, 3000);
        crewMatchService.updateMatchScore(testCrew2, 4000);

        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MatchDetailResponse> result = crewMatchService.getOngoingMatchDetail(testUser);

        // Then
        assertThat(result).isPresent();
        MatchDetailResponse response = result.get();

        // 매치 정보 확인
        assertThat(response.match().matchId()).isEqualTo(match.getId());
        assertThat(response.match().title()).isEqualTo("주간 배틀");

        // 내 크루 정보 확인
        assertThat(response.myCrewDetail().crewId()).isEqualTo(testCrew1.getId());
        assertThat(response.myCrewDetail().crewName()).isEqualTo("크루A");
        assertThat(response.myCrewDetail().totalDistance()).isEqualTo(8000);

        // 상대 크루 정보 확인
        assertThat(response.opponentCrewDetail().crewId()).isEqualTo(testCrew2.getId());
        assertThat(response.opponentCrewDetail().crewName()).isEqualTo("크루B");
        assertThat(response.opponentCrewDetail().totalDistance()).isEqualTo(4000);
    }

    @Test
    @DisplayName("멤버 기여도가 거리 내림차순으로 정렬된다")
    @Transactional
    void getOngoingMatchDetail_MemberContributions_SortedByDistanceDesc() {
        // Given
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash")
                .nickname("유저2")
                .build());

        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(user2)
                .status(MemberStatus.APPROVED)
                .build());

        User user3 = userRepository.save(User.builder()
                .email("user3@test.com")
                .passwordHash("hash")
                .nickname("유저3")
                .build());

        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(user3)
                .status(MemberStatus.APPROVED)
                .build());

        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        // 러닝 기록 (user2가 가장 많이, testUser가 중간, user3이 가장 적게)
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .startedAt(LocalDateTime.of(2025, 1, 12, 10, 0))
                .build());

        runningRepository.save(Running.builder()
                .user(user2)
                .crew(testCrew1)
                .distance(8000)
                .duration(2400)
                .startedAt(LocalDateTime.of(2025, 1, 12, 11, 0))
                .build());

        runningRepository.save(Running.builder()
                .user(user3)
                .crew(testCrew1)
                .distance(2000)
                .duration(900)
                .startedAt(LocalDateTime.of(2025, 1, 12, 12, 0))
                .build());

        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MatchDetailResponse> result = crewMatchService.getOngoingMatchDetail(testUser);

        // Then
        assertThat(result).isPresent();
        var contributions = result.get().myCrewDetail().memberContributions();
        assertThat(contributions).hasSize(3);

        // 순위 확인: user2(8000) > testUser(5000) > user3(2000)
        assertThat(contributions.get(0).nickname()).isEqualTo("유저2");
        assertThat(contributions.get(0).distance()).isEqualTo(8000);
        assertThat(contributions.get(0).rank()).isEqualTo(1);

        assertThat(contributions.get(1).nickname()).isEqualTo("테스터");
        assertThat(contributions.get(1).distance()).isEqualTo(5000);
        assertThat(contributions.get(1).rank()).isEqualTo(2);

        assertThat(contributions.get(2).nickname()).isEqualTo("유저3");
        assertThat(contributions.get(2).distance()).isEqualTo(2000);
        assertThat(contributions.get(2).rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("거리가 0인 멤버도 목록에 포함된다")
    @Transactional
    void getOngoingMatchDetail_IncludesZeroDistanceMembers() {
        // Given: user2는 러닝 기록이 없음
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash")
                .nickname("유저2")
                .build());

        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(user2)
                .status(MemberStatus.APPROVED)
                .build());

        CrewMatch match = crewMatchService.createOneOnOneMatch(
                "주간 배틀",
                testCrew1.getId(),
                testCrew2.getId(),
                LocalDateTime.of(2025, 1, 10, 0, 0),
                LocalDateTime.of(2025, 1, 20, 23, 59)
        );

        // testUser만 러닝
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .startedAt(LocalDateTime.of(2025, 1, 12, 10, 0))
                .build());

        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MatchDetailResponse> result = crewMatchService.getOngoingMatchDetail(testUser);

        // Then
        assertThat(result).isPresent();
        var contributions = result.get().myCrewDetail().memberContributions();
        assertThat(contributions).hasSize(2);

        // user2가 0km로 포함되어야 함
        assertThat(contributions.get(0).nickname()).isEqualTo("테스터");
        assertThat(contributions.get(0).distance()).isEqualTo(5000);
        assertThat(contributions.get(0).rank()).isEqualTo(1);

        assertThat(contributions.get(1).nickname()).isEqualTo("유저2");
        assertThat(contributions.get(1).distance()).isEqualTo(0);
        assertThat(contributions.get(1).rank()).isEqualTo(2);
    }
}