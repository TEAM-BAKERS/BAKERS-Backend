package com.example.bakersbackend.domain.crew.dto;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;

public record CrewHomeResponse(
        boolean hasCrew,
        CrewSummaryData crew
) {}

