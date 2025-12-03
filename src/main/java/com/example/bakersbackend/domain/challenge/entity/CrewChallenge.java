package com.example.bakersbackend.domain.challenge.entity;

import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "crew_challenge",
        indexes = {
                @Index(name = "idx_crew_challenge_crew_status", columnList = "crew_id, status"),
                @Index(name = "idx_crew_challenge_status_period", columnList = "status, start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewChallenge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "goal_distance", nullable = false)
    private Integer goalDistance;

    @Column(name = "type", nullable = true)
    private ChallengeType type;

    @Column(name = "goal_value")
    private Integer goalValue;

    // 크루 전체가 달성한 누적 거리 (미터 단위)
    @Column(name = "current_accumulated_distance", nullable = false)
    private Integer currentAccumulatedDistance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeStatus status;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    // 비즈니스 메서드: 크루 전체 누적 거리 증가
    public void addAccumulatedDistance(Integer distance) {
        if (distance == null || distance <= 0) {
            throw new IllegalArgumentException("추가할 거리는 양수여야 합니다.");
        }
        this.currentAccumulatedDistance += distance;
    }

    // 비즈니스 메서드: 목표 달성 체크 후 성공 처리
    public boolean checkAndMarkSuccessIfGoalReached() {
        if (this.currentAccumulatedDistance >= this.goalDistance && this.status == ChallengeStatus.ACTIVE) {
            this.status = ChallengeStatus.SUCCESS;
            return true;
        }
        return false;
    }

    // 비즈니스 메서드: 실패 처리
    public void markAsFailed() {
        this.status = ChallengeStatus.FAILED;
    }

    // 비즈니스 메서드: 활성 상태 확인
    public boolean isActive() {
        return this.status == ChallengeStatus.ACTIVE;
    }

    // 비즈니스 메서드: 기간 내 확인
    public boolean isWithinPeriod(LocalDateTime now) {
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }
}