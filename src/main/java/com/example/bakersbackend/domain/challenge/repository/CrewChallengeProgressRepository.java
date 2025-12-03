package com.example.bakersbackend.domain.challenge.repository;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.entity.CrewChallengeProgress;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrewChallengeProgressRepository extends JpaRepository<CrewChallengeProgress, Long> {

    /**
     * 비관적 락을 사용하여 챌린지와 유저로 진행률을 조회합니다.
     * 동시성 제어를 위해 PESSIMISTIC_WRITE 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    Optional<CrewChallengeProgress> findByChallengeAndUser(CrewChallenge challenge, User user);

    /**
     * 락 없이 챌린지와 유저로 진행률을 조회합니다.
     * 검증 및 조회 전용으로 사용합니다.
     */
    @Query("SELECT ccp FROM CrewChallengeProgress ccp WHERE ccp.challenge = :challenge AND ccp.user = :user")
    Optional<CrewChallengeProgress> findByChallengeAndUserWithoutLock(@Param("challenge") CrewChallenge challenge, @Param("user") User user);

    // 거리 기반 챌린지라 가정하고 contributedDistance 합산
    @Query("""
           select coalesce(sum(p.contributedDistance), 0)
           from CrewChallengeProgress p
           where p.challenge.id = :challengeId
           """)
    Integer sumContributedDistance(@Param("challengeId") Long challengeId);
}