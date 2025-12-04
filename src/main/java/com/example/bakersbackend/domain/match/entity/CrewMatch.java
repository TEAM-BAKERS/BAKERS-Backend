package com.example.bakersbackend.domain.match.entity;

import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "crew_match",
        indexes = {
                @Index(name = "idx_crew_match_period", columnList = "start_at, end_at"),
                @Index(name = "idx_crew_match_status", columnList = "match_status")
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

    // 매치 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", length = 20, nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.ONGOING;

    // 승자 (null = 진행중 또는 무승부)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_crew_id")
    private Crew winner;

    // 참가자 목록 (1:1 매칭용)
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CrewMatchParticipant> participants = new ArrayList<>();

    // 비즈니스 메서드: 현재 시간이 매치 기간 내인지 확인
    public boolean isOngoing(LocalDateTime now) {
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    // 비즈니스 메서드: 참가자 추가 (1:1 제약)
    public void addParticipant(CrewMatchParticipant participant) {
        if (this.participants.size() >= 2) {
            throw new IllegalStateException("1:1 매칭에는 2개 크루만 참가할 수 있습니다.");
        }
        this.participants.add(participant);
        participant.setMatch(this);
    }

    // 비즈니스 메서드: 승자 판정
    public void determineWinner() {
        if (this.participants.size() != 2) {
            throw new IllegalStateException("1:1 매칭은 정확히 2개 크루가 필요합니다.");
        }

        // 거리 순으로 정렬 (내림차순)
        List<CrewMatchParticipant> sorted = this.participants.stream()
                .sorted((p1, p2) -> Integer.compare(p2.getTotalDistance(), p1.getTotalDistance()))
                .toList();

        CrewMatchParticipant first = sorted.get(0);
        CrewMatchParticipant second = sorted.get(1);

        if (first.getTotalDistance() > second.getTotalDistance()) {
            this.winner = first.getCrew();
            this.status = MatchStatus.FINISHED;
        } else {
            // 동점
            this.winner = null;
            this.status = MatchStatus.DRAW;
        }
    }
}