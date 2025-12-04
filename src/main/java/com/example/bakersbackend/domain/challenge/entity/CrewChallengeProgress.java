package com.example.bakersbackend.domain.challenge.entity;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "crew_challenge_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_challenge_progress_challenge_user",
                        columnNames = {"challenge_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_crew_challenge_progress_challenge", columnList = "challenge_id"),
                @Index(name = "idx_crew_challenge_progress_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewChallengeProgress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private CrewChallenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 개인이 기여한 거리 (미터 단위)
    @Column(name = "contributed_distance", nullable = false)
    private Integer contributedDistance;

    // 평균 심박수
    @Column(name = "avg_heartrate")
    private Short avgHeartrate;

    // 비즈니스 메서드: 기여 거리 추가
    public void addDistance(Integer distance) {
        if (distance == null || distance <= 0) {
            throw new IllegalArgumentException("추가할 거리는 양수여야 합니다.");
        }
        this.contributedDistance += distance;
    }

    // 정적 팩토리 메서드: 새로운 진행률 생성
    public static CrewChallengeProgress createNew(CrewChallenge challenge, User user, Integer initialDistance) {
        if (initialDistance == null || initialDistance < 0) {
            throw new IllegalArgumentException("초기 거리는 0 이상이어야 합니다.");
        }
        return CrewChallengeProgress.builder()
                .challenge(challenge)
                .user(user)
                .contributedDistance(initialDistance)
                .build();
    }
}