package com.example.bakersbackend.domain.home.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.entity.CrewMember;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.home.dto.BattleLeagueSummary;
import com.example.bakersbackend.domain.home.dto.HomeResponse;
import com.example.bakersbackend.domain.home.dto.RecentCrewActivity;
import com.example.bakersbackend.domain.home.dto.TodayRunningRecord;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.entity.MatchStatus;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class HomeServiceTest {

    @Autowired
    private HomeService homeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    @Autowired
    private RunningRepository runningRepository;

    @Autowired
    private CrewMatchRepository crewMatchRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private User testUser;
    private Crew testCrew1;
    private Crew testCrew2;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        runningRepository.deleteAll();
        crewMemberRepository.deleteAll();
        crewMatchRepository.deleteAll();
        crewRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 사용자 생성
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .nickname("테스트유저")
                .build());

        // 테스트 크루 생성 (name은 최대 5자)
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
    @DisplayName("정상 케이스: 모든 데이터가 있는 경우")
    void getHome_Success_AllData() {
        // Given: 사용자가 크루에 가입
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        // 배틀 리그 생성
        LocalDateTime now = LocalDateTime.now(clock);
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("1월 배틀")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .status(MatchStatus.ONGOING)
                .build());

        match.addParticipant(CrewMatchParticipant.createNew(match, testCrew1, 0));
        match.addParticipant(CrewMatchParticipant.createNew(match, testCrew2, 0));
        crewMatchRepository.save(match);

        // 점수 업데이트
        CrewMatchParticipant p1 = match.getParticipants().get(0);
        CrewMatchParticipant p2 = match.getParticipants().get(1);
        p1.addDistance(10000);
        p2.addDistance(8000);

        // 오늘의 러닝 기록
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .pace(360)
                .startedAt(now)
                .build());

        // 크루의 최근 활동
        User user2 = userRepository.save(User.builder()
                .email("user2@test.com")
                .passwordHash("hash")
                .nickname("유저2")
                .build());

        runningRepository.save(Running.builder()
                .user(user2)
                .crew(testCrew1)
                .distance(3000)
                .duration(1200)
                .pace(400)
                .startedAt(now.minusHours(1))
                .build());

        entityManager.flush();
        entityManager.clear();

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response).isNotNull();

        // 배틀 리그 정보 확인
        BattleLeagueSummary battleLeague = response.battleLeague();
        assertThat(battleLeague).isNotNull();
        assertThat(battleLeague.myCrewName()).isEqualTo("크루A");
        assertThat(battleLeague.opponentCrewName()).isEqualTo("크루B");
        assertThat(battleLeague.myCrewDistance()).isEqualTo(10000);
        assertThat(battleLeague.opponentCrewDistance()).isEqualTo(8000);

        // 오늘의 러닝 기록 확인
        TodayRunningRecord todayRunning = response.todayRunning();
        assertThat(todayRunning).isNotNull();
        assertThat(todayRunning.distance()).isEqualTo(5000);
        assertThat(todayRunning.duration()).isEqualTo(1800);
        assertThat(todayRunning.pace()).isEqualTo(360);

        // 최근 활동 확인
        List<RecentCrewActivity> activities = response.recentActivities();
        assertThat(activities).isNotEmpty();
        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).nickname()).isEqualTo("테스트유저");
        assertThat(activities.get(1).nickname()).isEqualTo("유저2");
    }

    @Test
    @DisplayName("크루가 없는 경우")
    void getHome_NoCrew() {
        // Given: 사용자가 크루에 가입하지 않음

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.battleLeague()).isNull();
        assertThat(response.todayRunning()).isNull();
        assertThat(response.recentActivities()).isEmpty();
    }

    @Test
    @DisplayName("배틀 리그가 없는 경우")
    void getHome_NoBattleLeague() {
        // Given: 크루에 가입했지만 배틀 리그 없음
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.battleLeague()).isNull();
        assertThat(response.todayRunning()).isNull();
        assertThat(response.recentActivities()).isEmpty();
    }

    @Test
    @DisplayName("배틀 리그에 참가하지 않은 경우")
    void getHome_NotParticipatingInMatch() {
        // Given: 크루에 가입했지만 배틀 리그 불참
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        // 다른 크루들의 배틀 리그
        User otherUser = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash("hash")
                .nickname("다른유저")
                .build());

        Crew otherCrew1 = crewRepository.save(Crew.builder()
                .name("크루C")
                .owner(otherUser)
                .build());

        Crew otherCrew2 = crewRepository.save(Crew.builder()
                .name("크루D")
                .owner(otherUser)
                .build());

        LocalDateTime now = LocalDateTime.now(clock);
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("다른 배틀")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .status(MatchStatus.ONGOING)
                .build());

        match.addParticipant(CrewMatchParticipant.createNew(match, otherCrew1, 0));
        match.addParticipant(CrewMatchParticipant.createNew(match, otherCrew2, 0));
        crewMatchRepository.save(match);

        entityManager.flush();
        entityManager.clear();

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.battleLeague()).isNull(); // 내 크루가 참가하지 않음
    }

    @Test
    @DisplayName("오늘 러닝 기록이 없는 경우")
    void getHome_NoTodayRunning() {
        // Given: 크루에 가입, 배틀 리그 참가, 하지만 오늘 러닝 기록 없음
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        LocalDateTime now = LocalDateTime.now(clock);
        CrewMatch match = crewMatchRepository.save(CrewMatch.builder()
                .title("1월 배틀")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .status(MatchStatus.ONGOING)
                .build());

        match.addParticipant(CrewMatchParticipant.createNew(match, testCrew1, 0));
        match.addParticipant(CrewMatchParticipant.createNew(match, testCrew2, 0));
        crewMatchRepository.save(match);

        // 어제의 러닝 기록만 있음
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .pace(360)
                .startedAt(now.minusDays(1))
                .build());

        entityManager.flush();
        entityManager.clear();

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.battleLeague()).isNotNull();
        assertThat(response.todayRunning()).isNull(); // 오늘 기록 없음
    }

    @Test
    @DisplayName("크루 활동이 5개 이상일 때 최근 5개만 반환")
    void getHome_RecentActivities_Limit5() {
        // Given: 크루에 가입
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        LocalDateTime now = LocalDateTime.now(clock);

        // 7개의 러닝 기록 생성
        for (int i = 0; i < 7; i++) {
            runningRepository.save(Running.builder()
                    .user(testUser)
                    .crew(testCrew1)
                    .distance(1000 * (i + 1))
                    .duration(600)
                    .pace(360)
                    .startedAt(now.minusHours(i))
                    .build());
        }

        entityManager.flush();
        entityManager.clear();

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        assertThat(response.recentActivities()).hasSize(5); // 최대 5개만
    }

    @Test
    @DisplayName("오늘 러닝 기록이 여러 개일 때 합산하여 반환")
    void getHome_TodayRunning_SumAll() {
        // Given: 크루에 가입
        crewMemberRepository.save(CrewMember.builder()
                .crew(testCrew1)
                .user(testUser)
                .status(MemberStatus.APPROVED)
                .build());

        LocalDateTime now = LocalDateTime.now(clock);
        // 오늘 정오로 설정하여 날짜 경계 문제 방지
        LocalDateTime todayNoon = now.toLocalDate().atTime(12, 0);

        // 오늘 여러 러닝 기록 생성
        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(3000)
                .duration(1200)
                .pace(400)
                .startedAt(todayNoon.minusHours(2))
                .build());

        runningRepository.save(Running.builder()
                .user(testUser)
                .crew(testCrew1)
                .distance(5000)
                .duration(1800)
                .pace(360)
                .startedAt(todayNoon.plusHours(2))
                .build());

        entityManager.flush();
        entityManager.clear();

        // When
        HomeResponse response = homeService.getHome(testUser);

        // Then
        TodayRunningRecord todayRunning = response.todayRunning();
        assertThat(todayRunning).isNotNull();
        assertThat(todayRunning.distance()).isEqualTo(8000); // 3000 + 5000 합산
        assertThat(todayRunning.duration()).isEqualTo(3000); // 1200 + 1800 합산
        // 평균 페이스: 3000초 / (8000m / 1000) = 3000 / 8 = 375 초/km
        assertThat(todayRunning.pace()).isEqualTo(375);
    }
}
