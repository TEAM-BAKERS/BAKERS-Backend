package com.example.bakersbackend.domain.match.repository;

import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrewMatchParticipantRepository extends JpaRepository<CrewMatchParticipant, Long> {

    /**
     * 비관적 락을 사용하여 매치와 크루로 참가자를 조회합니다.
     * 동시성 제어를 위해 PESSIMISTIC_WRITE 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    Optional<CrewMatchParticipant> findByMatchAndCrew(CrewMatch match, Crew crew);

    /**
     * 락 없이 매치와 크루로 참가자를 조회합니다.
     * 검증 및 조회 전용으로 사용합니다.
     */
    @Query("SELECT cmp FROM CrewMatchParticipant cmp WHERE cmp.match = :match AND cmp.crew = :crew")
    Optional<CrewMatchParticipant> findByMatchAndCrewWithoutLock(@Param("match") CrewMatch match, @Param("crew") Crew crew);

    /**
     * 특정 매치의 모든 참가자를 총 거리 내림차순으로 조회합니다.
     */
    @Query("SELECT cmp FROM CrewMatchParticipant cmp " +
           "JOIN FETCH cmp.crew " +
           "WHERE cmp.match = :match " +
           "ORDER BY cmp.totalDistance DESC")
    List<CrewMatchParticipant> findByMatchOrderByTotalDistanceDesc(@Param("match") CrewMatch match);
}