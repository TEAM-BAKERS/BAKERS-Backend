package com.example.bakersbackend.domain.crew.dto;

import java.time.LocalDate;

public record CrewInfoData(
        LocalDate createdAt,      // 생성일 (날짜만)
        int memberCount,          // 현재 인원
        Integer maxMember         // 정원 (nullable)
) {}
