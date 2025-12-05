package com.example.bakersbackend.domain.home.dto;

public record RecentCrewActivity(
        String nickname,     // 이름
        Integer distance,    // 거리 (미터)
        Integer duration,    // 시간 (초)
        Integer pace         // 페이스 (초/km)
) {
}
