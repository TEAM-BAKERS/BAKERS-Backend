package com.example.bakersbackend.domain.match.repository;

import com.example.bakersbackend.domain.match.entity.CrewMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CrewMatchRepository extends JpaRepository<CrewMatch, Long> {

    /**
     * 현재 시간 기준으로 진행 중인 매치를 조회합니다.
     */
    @Query("SELECT cm FROM CrewMatch cm WHERE :now BETWEEN cm.startAt AND cm.endAt")
    Optional<CrewMatch> findOngoingMatch(@Param("now") LocalDateTime now);
}