package com.example.bakersbackend.domain.running.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RunningRecordRequest(
        @NotNull(message = "크루 ID는 필수입니다.")
        Long crewId,

        @NotNull(message = "거리는 필수입니다.")
        @Min(value = 1, message = "거리는 1m 이상이어야 합니다.")
        Integer distance,

        @NotNull(message = "시간은 필수입니다.")
        @Min(value = 1, message = "시간은 1초 이상이어야 합니다.")
        Integer duration,

        Short avgHeartrate,

        Integer pace
) {
}