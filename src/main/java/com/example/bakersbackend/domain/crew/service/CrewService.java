package com.example.bakersbackend.domain.crew.service;

import com.example.bakersbackend.domain.crew.dto.CrewListData;
import com.example.bakersbackend.domain.crew.dto.CrewListResponse;
import com.example.bakersbackend.domain.crew.dto.TagData;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.entity.CrewTag;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import com.example.bakersbackend.domain.crew.repository.CrewDistanceProjection;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;

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
}