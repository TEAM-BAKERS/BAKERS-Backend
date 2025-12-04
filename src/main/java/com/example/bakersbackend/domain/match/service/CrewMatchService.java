package com.example.bakersbackend.domain.match.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.entity.CrewMember;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.match.dto.CrewMatchDetail;
import com.example.bakersbackend.domain.match.dto.MatchDetailResponse;
import com.example.bakersbackend.domain.match.dto.MemberContribution;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.entity.MatchStatus;
import com.example.bakersbackend.domain.match.repository.CrewMatchParticipantRepository;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
import com.example.bakersbackend.domain.running.dto.UserDistanceProjection;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrewMatchService {

    private final CrewMatchRepository crewMatchRepository;
    private final CrewMatchParticipantRepository crewMatchParticipantRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final RunningRepository runningRepository;
    private final Clock clock;

    /**
     * 러닝 기록 후 배틀 리그 점수를 갱신합니다.
     * 동시성 제어를 위해 비관적 락을 사용합니다.
     */
    @Transactional
    public void updateMatchScore(Crew crew, Integer distance) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. 현재 진행 중인 매치 조회
        Optional<CrewMatch> matchOpt = crewMatchRepository.findOngoingMatch(now);

        // 2. Empty Handling: 진행 중인 매치가 없으면 종료
        if (matchOpt.isEmpty()) {
            log.debug("현재 진행 중인 배틀 리그가 없습니다. 점수 업데이트를 건너뜁니다.");
            return;
        }

        CrewMatch match = matchOpt.get();

        // 3. 참가자 조회 및 점수 업데이트
        updateParticipantScore(match, crew, distance);
    }

    /**
     * 크루의 배틀 리그 참가자 점수를 업데이트합니다.
     * 중요: 참가자가 아닌 경우 예외를 던지지 않고 조용히 넘어갑니다. (러닝 기록 롤백 방지)
     */
    private void updateParticipantScore(CrewMatch match, Crew crew, Integer distance) {
        // 비관적 락(PESSIMISTIC_WRITE)으로 참가자 조회 - 동시성 제어
        crewMatchParticipantRepository.findByMatchAndCrew(match, crew)
                .ifPresentOrElse(
                        participant -> {
                            // 참가자가 있으면 거리 증가
                            participant.addDistance(distance);
                            log.debug("배틀 리그 점수 업데이트: matchId={}, crewId={}, 추가={}m, 총합={}m",
                                    match.getId(), crew.getId(), distance, participant.getTotalDistance());
                        },
                        () -> {
                            // 참가자가 아니면 조용히 넘어감 (러닝 기록은 보호됨)
                            log.debug("크루 {}는 매치 {}의 참가자가 아닙니다. 점수 업데이트를 건너뜁니다.",
                                    crew.getId(), match.getId());
                        }
                );
    }

    /**
     * 1:1 매칭 생성 (관리자용)
     */
    @Transactional
    public CrewMatch createOneOnOneMatch(
            String title,
            Long crew1Id,
            Long crew2Id,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        // 이미 진행 중인 매치가 있는지 확인
        if (crewMatchRepository.existsByStatus(MatchStatus.ONGOING)) {
            throw new IllegalStateException("이미 진행 중인 매치가 있습니다.");
        }

        // 크루 조회
        Crew crew1 = crewRepository.findById(crew1Id)
                .orElseThrow(() -> new EntityNotFoundException("크루1을 찾을 수 없습니다. id=" + crew1Id));
        Crew crew2 = crewRepository.findById(crew2Id)
                .orElseThrow(() -> new EntityNotFoundException("크루2를 찾을 수 없습니다. id=" + crew2Id));

        // 매치 생성
        CrewMatch match = CrewMatch.builder()
                .title(title)
                .startAt(startAt)
                .endAt(endAt)
                .status(MatchStatus.ONGOING)
                .build();

        // 참가자 추가 (cascade로 자동 저장됨)
        match.addParticipant(CrewMatchParticipant.createNew(match, crew1, 0));
        match.addParticipant(CrewMatchParticipant.createNew(match, crew2, 0));

        CrewMatch savedMatch = crewMatchRepository.save(match);
        log.info("1:1 매치 생성: matchId={}, {}vs{}", savedMatch.getId(), crew1.getName(), crew2.getName());

        return savedMatch;
    }

    /**
     * 매치 종료 및 승자 판정
     */
    @Transactional
    public void finishMatch(Long matchId) {
        CrewMatch match = crewMatchRepository.findByIdWithParticipants(matchId)
                .orElseThrow(() -> new EntityNotFoundException("매치를 찾을 수 없습니다. id=" + matchId));

        match.determineWinner();
        log.info("매치 종료: matchId={}, status={}, winner={}",
                matchId, match.getStatus(),
                match.getWinner() != null ? match.getWinner().getName() : "무승부");
    }

    /**
     * 현재 진행 중인 배틀 리그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<CrewMatch> getOngoingMatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        return crewMatchRepository.findOngoingMatch(now);
    }

    /**
     * 현재 진행 중인 배틀 리그를 참가자와 함께 조회합니다 (N+1 방지).
     */
    @Transactional(readOnly = true)
    public Optional<CrewMatch> getOngoingMatchWithParticipants() {
        LocalDateTime now = LocalDateTime.now(clock);
        return crewMatchRepository.findOngoingMatchWithParticipants(now);
    }

    /**
     * 특정 매치의 참가자 순위를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewMatchParticipant> getMatchLeaderboard(CrewMatch match) {
        return crewMatchParticipantRepository.findByMatchOrderByTotalDistanceDesc(match);
    }

    /**
     * 진행 중인 배틀 리그 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<MatchDetailResponse> getOngoingMatchDetail(User user) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. 진행 중인 매치 조회 (참가자 Fetch Join)
        Optional<CrewMatch> matchOpt = crewMatchRepository.findOngoingMatchWithParticipants(now);
        if (matchOpt.isEmpty()) {
            return Optional.empty();
        }

        CrewMatch match = matchOpt.get();

        // 2. 사용자의 크루 확인 (참가자 중에서)
        Optional<Crew> myCrewOpt = match.getParticipants().stream()
                .map(CrewMatchParticipant::getCrew)
                .filter(crew -> crewMemberRepository.existsByCrewIdAndUserIdAndStatus(
                        crew.getId(),
                        user.getId(),
                        com.example.bakersbackend.domain.crew.entity.MemberStatus.APPROVED
                ))
                .findFirst();

        if (myCrewOpt.isEmpty()) {
            log.debug("사용자 {}는 진행 중인 배틀 리그의 참가자가 아닙니다.", user.getId());
            return Optional.empty();
        }

        Crew myCrew = myCrewOpt.get();

        // 3. 상대 크루 식별
        Crew opponentCrew = match.getParticipants().stream()
                .map(CrewMatchParticipant::getCrew)
                .filter(crew -> !crew.getId().equals(myCrew.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("1:1 매치에 상대 크루가 없습니다."));

        // 4. 각 크루의 상세 정보 생성
        CrewMatchDetail myCrewDetail = buildCrewMatchDetail(myCrew, match, true);  // 내 크루는 크루원 포함
        CrewMatchDetail opponentCrewDetail = buildCrewMatchDetail(opponentCrew, match, false);  // 상대 크루는 크루원 제외

        // 5. 응답 생성
        return Optional.of(MatchDetailResponse.of(match, myCrewDetail, opponentCrewDetail));
    }

    /**
     * 크루의 배틀 리그 상세 정보를 생성합니다.
     * @param includeMemberDetails true: 크루원 기여도 포함, false: 총 거리만 포함
     */
    private CrewMatchDetail buildCrewMatchDetail(Crew crew, CrewMatch match, boolean includeMemberDetails) {
        // 1. 매치 참가자에서 totalDistance 가져오기
        Integer totalDistance = match.getParticipants().stream()
                .filter(p -> p.getCrew().getId().equals(crew.getId()))
                .findFirst()
                .map(CrewMatchParticipant::getTotalDistance)
                .orElse(0);

        // 2. 크루원 정보 제외 시 빈 리스트 반환
        if (!includeMemberDetails) {
            return CrewMatchDetail.of(crew, totalDistance, List.of());
        }

        // 3. 전체 승인된 멤버 조회
        List<CrewMember> allMembers = crewMemberRepository.findApprovedMembersWithUser(crew);

        // 4. 기간 내 개인 거리 집계
        List<UserDistanceProjection> userDistances = runningRepository.sumDistanceByCrewAndPeriod(
                crew,
                match.getStartAt(),
                match.getEndAt()
        );

        // 5. Map으로 변환 (userId -> distance)
        Map<Long, Integer> distanceMap = userDistances.stream()
                .collect(Collectors.toMap(
                        UserDistanceProjection::getUserId,
                        UserDistanceProjection::getTotalDistance
                ));

        // 6. 멤버별 기여도 생성 (거리 없으면 0)
        List<MemberContribution> contributions = allMembers.stream()
                .map(member -> {
                    Long userId = member.getUser().getId();
                    String nickname = member.getUser().getNickname();
                    Integer distance = distanceMap.getOrDefault(userId, 0);
                    return new MemberContribution(userId, nickname, distance, 0); // rank는 나중에 설정
                })
                .sorted((c1, c2) -> Integer.compare(c2.distance(), c1.distance())) // 거리 내림차순
                .toList();

        // 7. 순위 부여
        List<MemberContribution> rankedContributions = new ArrayList<>();
        int rank = 1;
        for (MemberContribution contribution : contributions) {
            rankedContributions.add(MemberContribution.of(
                    contribution.userId(),
                    contribution.nickname(),
                    contribution.distance(),
                    rank++
            ));
        }

        return CrewMatchDetail.of(crew, totalDistance, rankedContributions);
    }
}