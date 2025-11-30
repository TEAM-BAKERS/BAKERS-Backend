package com.example.bakersbackend.domain.match.entity;

import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "crew_match_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_match_participant_match_crew",
                        columnNames = {"match_id", "crew_id"}
                )
        },
        indexes = {
                @Index(name = "idx_crew_match_participant_match", columnList = "match_id"),
                @Index(name = "idx_crew_match_participant_crew", columnList = "crew_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewMatchParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private CrewMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    // 크루의 총 누적 거리 (미터 단위)
    @Column(name = "total_distance", nullable = false)
    private Integer totalDistance;

    // 비즈니스 메서드: 거리 추가
    public void addDistance(Integer distance) {
        if (distance == null || distance <= 0) {
            throw new IllegalArgumentException("추가할 거리는 양수여야 합니다.");
        }
        this.totalDistance += distance;
    }

    // 정적 팩토리 메서드: 새로운 참가자 생성
    public static CrewMatchParticipant createNew(CrewMatch match, Crew crew, Integer initialDistance) {
        if (initialDistance == null || initialDistance < 0) {
            throw new IllegalArgumentException("초기 거리는 0 이상이어야 합니다.");
        }
        return CrewMatchParticipant.builder()
                .match(match)
                .crew(crew)
                .totalDistance(initialDistance)
                .build();
    }
}