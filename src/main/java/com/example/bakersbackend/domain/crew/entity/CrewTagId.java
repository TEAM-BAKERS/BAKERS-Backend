package com.example.bakersbackend.domain.crew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CrewTagId implements Serializable {

    @Column(name = "crew_id")
    private Long crewId;

    @Column(name = "tag_id")
    private Long tagId;
}
