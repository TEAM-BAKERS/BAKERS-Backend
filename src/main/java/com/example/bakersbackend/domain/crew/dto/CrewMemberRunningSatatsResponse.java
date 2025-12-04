package com.example.bakersbackend.domain.crew.dto;

public record CrewMemberRunningSatatsResponse(
        Long userId,
        String nickname,
        String imageUrl,
        Long weeklyDistance,   // 주간 거리 합계 (m)
        Long monthlyDistance   // 월간 거리 합계 (m)
) {
    // JPQL 에서 canonical 생성자를 호출하니까, 여기서 null -> 0 처리
    public CrewMemberRunningSatatsResponse {
        weeklyDistance = (weeklyDistance == null) ? 0L : weeklyDistance;
        monthlyDistance = (monthlyDistance == null) ? 0L : monthlyDistance;
    }
}