package com.example.bakersbackend.domain.match.repository;

import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.MatchStatus;
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

    /**
     * 특정 상태의 매치가 존재하는지 확인합니다.
     */
    boolean existsByStatus(MatchStatus status);

    /**
     * 매치와 참가자를 Fetch Join으로 조회 (N+1 방지)
     */
    @Query("SELECT cm FROM CrewMatch cm " +
           "LEFT JOIN FETCH cm.participants p " +
           "LEFT JOIN FETCH p.crew " +
           "WHERE cm.id = :matchId")
    Optional<CrewMatch> findByIdWithParticipants(@Param("matchId") Long matchId);

    /**
     * 진행 중인 매치를 참가자와 함께 조회 (N+1 방지)
     */
    @Query("SELECT cm FROM CrewMatch cm " +
           "LEFT JOIN FETCH cm.participants p " +
           "LEFT JOIN FETCH p.crew " +
           "WHERE :now BETWEEN cm.startAt AND cm.endAt")
    Optional<CrewMatch> findOngoingMatchWithParticipants(@Param("now") LocalDateTime now);
}