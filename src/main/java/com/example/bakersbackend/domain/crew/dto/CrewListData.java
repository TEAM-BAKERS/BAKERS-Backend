package com.example.bakersbackend.domain.crew.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CrewListData(
        Long groupId,
        String name,
        String intro,
        List<TagData> tags,
        Integer current,
        Integer max,
        Double distance,
        String imgUrl
) {
}
