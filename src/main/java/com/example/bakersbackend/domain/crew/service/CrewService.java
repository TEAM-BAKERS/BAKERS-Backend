package com.example.bakersbackend.domain.crew.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.crew.dto.CrewListData;
import com.example.bakersbackend.domain.crew.dto.CrewListResponse;
import com.example.bakersbackend.domain.crew.dto.CrewSearchResponse;
import com.example.bakersbackend.domain.crew.dto.TagData;
import com.example.bakersbackend.domain.crew.entity.*;
import com.example.bakersbackend.domain.crew.repository.CrewDistanceProjection;
import com.example.bakersbackend.domain.crew.repository.CrewMemberRepository;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    public List<CrewSearchResponse> searchKeyword(String keyword) {

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
}