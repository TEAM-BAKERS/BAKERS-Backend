package com.example.bakersbackend.domain.home.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.home.dto.*;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
import com.example.bakersbackend.domain.running.entity.Running;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final RunningRepository runningRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewMatchRepository crewMatchRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public HomeResponse getHome(User user) {
        // 1. 사용자의 크루 조회
        Optional<Crew> myCrewOpt = getUserCrew(user);

        // 2. 배틀 리그 정보 조회
        BattleLeagueSummary battleLeague = myCrewOpt
                .map(this::getBattleLeagueSummary)
                .orElse(null);

        // 3. 오늘의 나의 러닝 기록 조회
        TodayRunningRecord todayRunning = getTodayRunning(user);

        // 4. 그룹 활동 (최근 5명) 조회
        List<RecentCrewActivity> recentActivities = myCrewOpt
                .map(crew -> getRecentActivities(crew))
                .orElse(List.of());

        return new HomeResponse(battleLeague, todayRunning, recentActivities);
    }

    /**
     * 사용자의 크루 조회 (APPROVED 상태)
     */
    private Optional<Crew> getUserCrew(User user) {
        // TODO: 1인 1크루 정책이므로 첫 번째 크루 반환
        // 추후 여러 크루 지원 시 수정 필요
        return crewMemberRepository.findAll().stream()
                .filter(cm -> cm.getUser().getId().equals(user.getId()))
                .filter(cm -> cm.getStatus() == MemberStatus.APPROVED)
                .map(cm -> cm.getCrew())
                .findFirst();
    }

    /**
     * 배틀 리그 간략 정보 조회
     */
    private BattleLeagueSummary getBattleLeagueSummary(Crew myCrew) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 진행 중인 배틀 리그 조회
        Optional<CrewMatch> matchOpt = crewMatchRepository.findOngoingMatchWithParticipants(now);

        if (matchOpt.isEmpty()) {
            return null;
        }

        CrewMatch match = matchOpt.get();

        // 내 크루가 참가 중인지 확인
        Optional<CrewMatchParticipant> myParticipantOpt = match.getParticipants().stream()
                .filter(p -> p.getCrew().getId().equals(myCrew.getId()))
                .findFirst();

        if (myParticipantOpt.isEmpty()) {
            return null; // 내 크루가 참가하지 않음
        }

        CrewMatchParticipant myParticipant = myParticipantOpt.get();

        // 상대 크루 찾기
        Optional<CrewMatchParticipant> opponentOpt = match.getParticipants().stream()
                .filter(p -> !p.getCrew().getId().equals(myCrew.getId()))
                .findFirst();

        if (opponentOpt.isEmpty()) {
            return null; // 1:1 매치에서 상대가 없음
        }

        CrewMatchParticipant opponent = opponentOpt.get();

        return new BattleLeagueSummary(
                myCrew.getName(),
                opponent.getCrew().getName(),
                myParticipant.getTotalDistance(),
                opponent.getTotalDistance()
        );
    }

    /**
     * 오늘의 나의 러닝 기록 조회 (하루 전체 합산)
     */
    private TodayRunningRecord getTodayRunning(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);

        List<Running> todayRunnings = runningRepository.findTodayRunningsByUser(
                user.getId(),
                startOfDay,
                endOfDay
        );

        if (todayRunnings.isEmpty()) {
            return null; // 오늘 기록 없음
        }

        // 오늘 하루 전체 합산
        int totalDistance = todayRunnings.stream()
                .mapToInt(Running::getDistance)
                .sum();

        int totalDuration = todayRunnings.stream()
                .mapToInt(Running::getDuration)
                .sum();

        // 평균 페이스 계산 (초/km)
        int averagePace = totalDistance > 0
                ? (int) ((double) totalDuration / (totalDistance / 1000.0))
                : 0;

        return new TodayRunningRecord(
                totalDistance,
                totalDuration,
                averagePace
        );
    }

    /**
     * 크루의 최근 활동 5개 조회
     */
    private List<RecentCrewActivity> getRecentActivities(Crew crew) {
        List<Running> recentRunnings = runningRepository.findRecentRunningsByCrew(crew.getId());

        return recentRunnings.stream()
                .map(r -> new RecentCrewActivity(
                        r.getUser().getNickname(),
                        r.getDistance(),
                        r.getDuration(),
                        r.getPace()
                ))
                .toList();
    }
}
