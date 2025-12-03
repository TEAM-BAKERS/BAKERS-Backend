package com.example.bakersbackend.domain.running.repository;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.dto.CrewTotalStatsData;
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
}