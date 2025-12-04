package com.example.bakersbackend.domain.challenge.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateChallengeRequest(
        @NotBlank(message = "챌린지 제목은 필수입니다.")
        String title,

        String description,

        @NotNull(message = "목표 값은 필수입니다.")
        @Min(value = 1000, message = "목표 값은 최소 1000 이상이어야 합니다.")
        Integer goalValue,

        @NotNull(message = "종료 날짜는 필수입니다.")
        @Future(message = "종료 날짜는 미래여야 합니다.")
        LocalDateTime endDate
) {
}