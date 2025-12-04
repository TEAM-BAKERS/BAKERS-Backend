package com.example.bakersbackend.domain.crew.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "crew_tags",
        indexes = {
                @Index(name = "idx_crew_tags_tag_id", columnList = "tag_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewTag {

    @EmbeddedId
    private CrewTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("crewId")  // CrewTagId.crewId 매핑
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")   // CrewTagId.tagId 매핑
    @JoinColumn(name = "tag_id", nullable = false)
    private Tags tag;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.id == null) {
            this.id = new CrewTagId(
                    crew != null ? crew.getId() : null,
                    tag != null ? tag.getId() : null
            );
        }
    }
}
