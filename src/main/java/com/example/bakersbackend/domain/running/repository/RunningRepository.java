package com.example.bakersbackend.domain.running.repository;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.dto.CrewTotalStatsData;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.running.dto.UserDistanceProjection;
import com.example.bakersbackend.domain.running.entity.Running;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RunningRepository extends JpaRepository<Running, Long> {
    // 크루 전체 누적 거리/시간
    @Query("""
           select new com.example.bakersbackend.domain.crew.dto.CrewTotalStatsData(
               coalesce(sum(r.distance), 0),
               coalesce(sum(r.duration), 0)
           )
           from Running r
           where r.crew.id = :crewId
           """)
    CrewTotalStatsData findCrewTotalStats(@Param("crewId") Long crewId);

    // 오늘 달린 멤버 목록 (중복 제거)
    @Query("""
           select distinct r.user
           from Running r
           where r.crew.id = :crewId
             and r.startedAt between :start and :end
           """)
    List<User> findTodayRunners(@Param("crewId") Long crewId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    // 유저별 총 거리 (m 합계)
    @Query("select coalesce(sum(r.distance), 0) " +
            "from Running r " +
            "where r.user.id = :userId")
    Long sumDistanceByUserId(@Param("userId") Long userId);

    // 유저별 총 시간 (sec 합계)
    @Query("select coalesce(sum(r.duration), 0) " +
            "from Running r " +
            "where r.user.id = :userId")
    Long sumDurationByUserId(@Param("userId") Long userId);

    // 유저별 러닝 수
    @Query("select count(r) " +
            "from Running r " +
            "where r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // 크루 기간별 유저별 거리 합계 (배틀 리그 개인 기여도용)
    @Query("""
            SELECT r.user.id as userId, SUM(r.distance) as totalDistance
            FROM Running r
            WHERE r.crew = :crew
              AND r.startedAt BETWEEN :startAt AND :endAt
            GROUP BY r.user.id
            """)
    List<UserDistanceProjection> sumDistanceByCrewAndPeriod(
            @Param("crew") Crew crew,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    // 오늘의 특정 사용자 러닝 기록 조회 (가장 최근 1개)
    @Query("""
            SELECT r
            FROM Running r
            WHERE r.user.id = :userId
              AND r.startedAt BETWEEN :start AND :end
            ORDER BY r.createdAt DESC
            LIMIT 1
            """)
    Running findTodayRunningByUser(@Param("userId") Long userId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    // 오늘 하루 러닝 기록 합산 조회 (총 거리, 총 시간)
    @Query("""
            SELECT r
            FROM Running r
            WHERE r.user.id = :userId
              AND r.startedAt BETWEEN :start AND :end
            """)
    List<Running> findTodayRunningsByUser(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // 크루의 최근 러닝 기록 조회 (최대 5개)
    @Query("""
            SELECT r
            FROM Running r
            WHERE r.crew.id = :crewId
            ORDER BY r.startedAt DESC
            LIMIT 5
            """)
    List<Running> findRecentRunningsByCrew(@Param("crewId") Long crewId);
}