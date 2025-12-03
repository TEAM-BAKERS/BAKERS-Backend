package com.example.bakersbackend.domain.match.entity;

import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "crew_match",
        indexes = {
                @Index(name = "idx_crew_match_period", columnList = "start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewMatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    // 비즈니스 메서드: 현재 시간이 매치 기간 내인지 확인
    public boolean isOngoing(LocalDateTime now) {
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }
}