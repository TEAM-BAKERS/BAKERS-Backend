package com.example.bakersbackend.domain.running.entity;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.entity.Crew;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "running",
        indexes = {
                @Index(name = "idx_running_user_started_at", columnList = "user_id, started_at"),
                @Index(name = "idx_running_group_started_at", columnList = "group_id, started_at"),
                @Index(name = "idx_running_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Running {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // group_id -> 실제로는 현재 설계 상 '크루'를 가리키는 FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    // 거리(미터)
    @Column(name = "distance", nullable = false)
    private Integer distance;

    // 시간(초)
    @Column(name = "duration", nullable = false)
    private Integer duration;

    // 페이스(초/1km) - 계산값, null 허용
    @Column(name = "pace")
    private Integer pace;

    // 평균 심박수
    @Column(name = "avg_heartrate")
    private Short avgHeartrate;

    // 운동 시작 시각
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // 기록 생성 시각
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
