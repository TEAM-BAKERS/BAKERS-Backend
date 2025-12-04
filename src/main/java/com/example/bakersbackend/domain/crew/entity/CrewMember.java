package com.example.bakersbackend.domain.crew.entity;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "crew_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_member_crew_user",
                        columnNames = {"crew_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_crew_member_crew_id", columnList = "crew_id"),
                @Index(name = "idx_crew_member_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // crew_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    // user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.APPROVED;
}
