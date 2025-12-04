package com.example.bakersbackend.domain.running.dto;

import com.example.bakersbackend.domain.running.entity.Running;

import java.time.LocalDateTime;

public record RunningRecordResponse(
        Long runningId,
        Long userId,
        Long crewId,
        Integer distance,
        Integer duration,
        Short avgHeartrate,
        Integer pace,
        LocalDateTime startedAt,
        LocalDateTime createdAt
) {
    public static RunningRecordResponse from(Running running) {
        return new RunningRecordResponse(
                running.getId(),
                running.getUser().getId(),
                running.getCrew().getId(),
                running.getDistance(),
                running.getDuration(),
                running.getAvgHeartrate(),
                running.getPace(),
                running.getStartedAt(),
                running.getCreatedAt()
        );
    }
}