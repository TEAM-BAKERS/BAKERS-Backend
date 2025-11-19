package com.example.bakersbackend.domain.crew.entity;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "crew",
        indexes = {
                @Index(name = "idx_crew_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crew extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // owner_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String intro;

    @Column(name = "img_url", columnDefinition = "text")
    private String imgUrl;

    // tinyint -> Integer로 매핑
    @Column(name = "max")
    private Integer max;

    // 멤버 목록
    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewMember> members = new ArrayList<>();

    // 태그 매핑
    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewTag> crewTags = new ArrayList<>();
}
