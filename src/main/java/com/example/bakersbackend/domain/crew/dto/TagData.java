package com.example.bakersbackend.domain.crew.dto;

import lombok.Builder;

@Builder
public record TagData (
        Long tagId,
        String name
){
}
