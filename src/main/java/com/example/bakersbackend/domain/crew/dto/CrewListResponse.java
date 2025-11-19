package com.example.bakersbackend.domain.crew.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CrewListResponse(
        List<CrewListData> groupList,
        Integer count
){
}
