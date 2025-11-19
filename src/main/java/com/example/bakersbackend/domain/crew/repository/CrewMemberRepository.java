package com.example.bakersbackend.domain.crew.repository;

import com.example.bakersbackend.domain.crew.entity.CrewMember;
import com.example.bakersbackend.domain.crew.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    // 사용자가 해당 크루에 특정 상태(예: APPROVED, PENDING)로 가입했는지 여부
    boolean existsByCrewIdAndUserIdAndStatus(Long crewId, Long userId, MemberStatus status);

    // 1인 1크루(이미 다른 크루에 APPROVED 되어 있는지 확인하고 싶으면)
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);
}
