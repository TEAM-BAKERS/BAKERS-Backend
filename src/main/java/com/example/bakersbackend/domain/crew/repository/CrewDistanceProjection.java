package com.example.bakersbackend.domain.crew.repository;

public interface CrewDistanceProjection {
    Long getCrewId();
    Integer getTotalDistance();   // running.distance int(미터) 기준
}
