package com.example.bakersbackend.domain.challenge.repository;

import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.crew.entity.Crew;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrewChallengeRepository extends JpaRepository<CrewChallenge, Long> {

    /**
     * 비관적 락을 사용하여 크루의 활성 챌린지를 조회합니다.
     * 동시성 제어를 위해 PESSIMISTIC_WRITE 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT cc FROM CrewChallenge cc WHERE cc.crew = :crew AND cc.status = :status")
    Optional<CrewChallenge> findActiveChallengeByCrewWithLock(
            @Param("crew") Crew crew,
            @Param("status") ChallengeStatus status
    );

    /**
     * 락 없이 크루의 활성 챌린지를 조회합니다.
     */
    Optional<CrewChallenge> findByCrewAndStatus(Crew crew, ChallengeStatus status);

    /**
     * 크루의 모든 챌린지를 최신순으로 조회합니다.
     */
    List<CrewChallenge> findByCrewOrderByCreatedAtDesc(Crew crew);
}