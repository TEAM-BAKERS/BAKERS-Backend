package com.example.bakersbackend.domain.crew.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeProgressRepository;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeRepository;
import com.example.bakersbackend.domain.crew.dto.*;
import com.example.bakersbackend.domain.crew.entity.*;
import com.example.bakersbackend.domain.crew.repository.CrewDistanceProjection;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final UserRepository userRepository;
    private final RunningRepository runningRepository;
    private final CrewChallengeRepository crewChallengeRepository;
    private final CrewChallengeProgressRepository crewChallengeProgressRepository;

    // 그룹 조회
    public CrewListResponse getAllGroups() {
        // 1) 크루 리스트 (createdAt DESC)
        List<Crew> crews = crewRepository.findAllByOrderByCreatedAtDesc();

        // 2) 크루별 거리 합계 조회 (running.distance SUM)
        Map<Long, Integer> distanceMap = crewRepository.findCrewTotalDistances().stream()
                .collect(Collectors.toMap(
                        CrewDistanceProjection::getCrewId,
                        p -> p.getTotalDistance() != null ? p.getTotalDistance() : 0
                ));

        // 3) Crew → CrewListData 매핑
        List<CrewListData> groupList = crews.stream()
                .map(crew -> {
                    // 태그 매핑
                    List<TagData> tagDataList = crew.getCrewTags().stream()
                            .map(CrewTag::getTag)
                            .map(tag -> TagData.builder()
                                    .tagId(tag.getId())
                                    .name(tag.getName())
                                    .build())
                            .toList();

                    // 현재 인원 (APPROVED 멤버만 카운트)
                    int currentCount = (int) crew.getMembers().stream()
                            .filter(member -> member.getStatus() == MemberStatus.APPROVED)
                            .count();

                    // 거리 (미터 단위 합계)
                    Integer totalDistanceMeter = distanceMap.getOrDefault(crew.getId(), 0);

                    return CrewListData.builder()
                            .groupId(crew.getId())
                            .name(crew.getName())
                            .intro(crew.getIntro())
                            .tags(tagDataList)
                            .current(currentCount)
                            .max(crew.getMax())
                            // m → km 로
                            .distance(totalDistanceMeter / 1000.0)
                            .imgUrl(crew.getImgUrl())
                            .build();
                })
                .toList();

        return CrewListResponse.builder()
                .count(groupList.size())
                .groupList(groupList)
                .build();
    }

    // 크루 가입
    @Transactional
    public ResponseEntity<?> signUpGroups(Long crewId, Long userId) {
        // JSON 응답
        Map<String, Object> response = new HashMap<>();

        // 1. 크루 존재 여부 체크
        Optional<Crew> crewOptional = crewRepository.findById(crewId);
        if (crewOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "존재하지 않는 크루입니다.");
            response.put("crewId", crewId);

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }
        Crew crew = crewOptional.get();

        // 2. 유저 존재 여부 체크
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "존재하지 않는 유저입니다.");
            response.put("userId", userId);

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }
        User user = userOptional.get();

        // 3. 이미 가입됐는지 체크
        boolean joined = crewMemberRepository
                .existsByUserIdAndStatus(userId, MemberStatus.APPROVED);

        if (joined) {
            response.put("success", false);
            response.put("message", "이미 크루에 가입되어 있습니다.");
            response.put("crewId", crewId);

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(response);
        }

        // 4. crew_member INSERT
        CrewMember crewMember = CrewMember.builder()
                .crew(crew)
                .user(user)
                .role(MemberRole.MEMBER)
                .status(MemberStatus.APPROVED)
                .build();
        crewMemberRepository.save(crewMember);

        // 5. users.current_crew_id 업데이트
        user.setCurrentGroupId(crew.getId());
        userRepository.save(user);

        // 6. 성공 응답
        response.put("success", true);
        response.put("message", "크루 가입 완료!");
        response.put("crewId", crewId);

        return ResponseEntity.ok(response);
    }

    // 검색어 자동완성
    public List<CrewSearchResponse> autocomplete(String keyword) {

        // 빈 문자열 들어오면 바로 빈 리스트 반환
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return crewRepository
                .findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(keyword)
                .stream()
                .map(CrewSearchResponse::from)
                .toList();
    }

    // 크루 생성
    public Map<String, Object> saveCrew(Long userId, CrewCreateRequest req) throws IOException {
        // JSON 응답
        Map<String, Object> response = new HashMap<>();

        // 1. 해당 유저가 이미 승인된 크루에 가입되어 있는지 검증
        boolean exists = crewMemberRepository.existsByUserIdAndStatus(userId, MemberStatus.APPROVED);
        if (exists) {
            response.put("success", false);
            response.put("message", "이미 크루에 가입되어 있습니다.");
            return response;
        }

        // 2. 유저 조회 (없으면 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. userId=" + userId));

        // 3. 크루 생성 및 매핑
        Crew crew = Crew.builder()
                .name(req.name())
                .intro(req.intro())
                .max(req.max())
                .owner(user)
                .build();

        crewRepository.save(crew);

        // 4. 크루 멤버(오너 본인) 같이 등록하기 (원하면)
        CrewMember crewMember = CrewMember.builder()
                .crew(crew)
                .user(user)
                .status(MemberStatus.APPROVED)   // 기본값: 승인
                .role(MemberRole.LEADER)
                .build();

        crewMemberRepository.save(crewMember);

        // 5. 응답 데이터 구성
        response.put("success", true);
        response.put("message", "크루가 생성되었습니다.");
        response.put("crewId", crew.getId());

        return response;
    }

    /**
     * 내 크루 화면 데이터 조회
     */
    public CrewHomeResponse getMyCrew(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없음. id=" + userId));

        Long crewId = user.getCurrentGroupId();
        if (crewId == null) {
            // 크루 미가입 → 왼쪽 화면
            return new CrewHomeResponse(false, null);
        }

        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new IllegalStateException("현재 크루 정보를 찾을 수 없음. crewId=" + crewId));

        // 1) 크루 전체 누적 거리 / 시간
        CrewTotalStatsData totals = runningRepository.findCrewTotalStats(crewId);

        long totalDistanceMeter = totals.totalDistanceMeter();
        long totalDurationSec = totals.totalDurationSec();

        double totalKm = totalDistanceMeter / 1000.0;
        long totalMinutes = totalDurationSec / 3600;

        // 2) 현재 진행 중인 챌린지 (팀 챌린지)
        LocalDateTime now = LocalDateTime.now();
        var activeChallenges = crewChallengeRepository.findActiveChallenges(crewId, now);

        CrewChallengeData teamChallengeData = null;
        int goalAchieveRate = 0;

        if (!activeChallenges.isEmpty()) {
            CrewChallenge challenge = activeChallenges.get(0); // 가장 최근 것 하나만 사용

            int currentValue = 0;
            int progressRate = 0;

            // 여기서는 거리 기반(DISTANCE) 챌린지만 예시로 구현
            if (challenge.getType() == ChallengeType.DISTANCE) {
                Integer contributed = crewChallengeProgressRepository
                        .sumContributedDistance(challenge.getId());
                currentValue = contributed != null ? contributed : 0;
            }

            if (challenge.getGoalValue() > 0) {
                progressRate = (int) Math.round(
                        100.0 * currentValue / challenge.getGoalValue()
                );
            }

            teamChallengeData = new CrewChallengeData(
                    challenge.getId(),
                    challenge.getType(),
                    challenge.getGoalValue(),
                    currentValue,
                    progressRate,
                    challenge.getStartAt(),
                    challenge.getEndAt()
            );

            goalAchieveRate = progressRate;
        }

        // 3) 오늘 달린 멤버
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<User> todayRunnerEntities = runningRepository
                .findTodayRunners(crewId, startOfDay, endOfDay);

        List<TodayRunnerData> todayMembers = todayRunnerEntities.stream()
                .map(u -> new TodayRunnerData(
                        u.getId(),
                        u.getNickname(),
                        u.getImageUrl()  // 프로필 URL 필드 이름은 엔티티에 맞게 수정
                ))
                .toList();

        // 4) 크루 정보 (생성일, 인원수, 정원)
        int memberCount = crewMemberRepository.countByCrewIdAndStatus(
                crewId, MemberStatus.APPROVED
        );

        CrewStatsData statsData = new CrewStatsData(
                totalKm,
                totalMinutes,
                goalAchieveRate
        );

        CrewInfoData infoData = new CrewInfoData(
                crew.getCreatedAt().toLocalDate(),
                memberCount,
                crew.getMax()
        );

        CrewSummaryData crewSummaryData = new CrewSummaryData(
                crew.getId(),
                crew.getName(),
                crew.getIntro(),
                crew.getImgUrl(),
                statsData,
                teamChallengeData,
                todayMembers,
                infoData
        );

        return new CrewHomeResponse(true, crewSummaryData);
    }


    // 크루 멤버 달린 기록
    public List<CrewMemberRunningSatatsResponse> getCrewMemberStats(Long crewId) {
        LocalDateTime now = LocalDateTime.now();

        // 이번 주 월요일 00:00
        LocalDate today = now.toLocalDate();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();

        // 이번 달 1일 00:00
        LocalDate monthStartDate = today.withDayOfMonth(1);
        LocalDateTime monthStart = monthStartDate.atStartOfDay();

        return crewMemberRepository.findCrewMemberRunningStats(
                crewId,
                weekStart,
                monthStart,
                now
        );
    }
}