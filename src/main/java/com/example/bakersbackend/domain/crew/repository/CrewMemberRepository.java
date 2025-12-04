package com.example.bakersbackend.domain.crew.repository;

import com.example.bakersbackend.domain.crew.dto.CrewMemberRunningSatatsResponse;
import com.example.bakersbackend.domain.crew.entity.CrewMember;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    // 사용자가 해당 크루에 특정 상태(예: APPROVED, PENDING)로 가입했는지 여부
    boolean existsByCrewIdAndUserIdAndStatus(Long crewId, Long userId, MemberStatus status);

    // 1인 1크루(이미 다른 크루에 APPROVED 되어 있는지 확인하고 싶으면)
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);

    // 크루 가입 인원
    int countByCrewIdAndStatus(Long crewId, MemberStatus status);

    // 크루 멤버 달린 기록
    @Query("""
        select new com.example.bakersbackend.domain.crew.dto.CrewMemberRunningSatatsResponse(
            u.id,
            u.nickname,
            u.imageUrl,
            sum(case when r.startedAt between :weekStart and :now then r.distance else 0 end),
            sum(case when r.startedAt between :monthStart and :now then r.distance else 0 end)
        )
        from CrewMember cm
            join cm.user u
            left join Running r
                on r.user = u
               and r.crew = cm.crew
               and r.startedAt between :monthStart and :now
        where cm.crew.id = :crewId
          and cm.status = com.example.bakersbackend.domain.crew.entity.MemberStatus.APPROVED
        group by u.id, u.nickname, u.imageUrl
        order by u.nickname
        """)
    List<CrewMemberRunningSatatsResponse> findCrewMemberRunningStats(
            @Param("crewId") Long crewId,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("now") LocalDateTime now
    );
}
