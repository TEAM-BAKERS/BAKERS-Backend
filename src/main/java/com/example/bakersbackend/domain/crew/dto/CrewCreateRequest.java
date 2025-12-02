package com.example.bakersbackend.domain.crew.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;


// 크루 생성 DTO
@Builder
public record CrewCreateRequest(
        @Size(max = 5, message = "크루 이름은 최대 5글자까지 가능합니다.")
        String name,

        String intro,
        Integer max
) {
}
