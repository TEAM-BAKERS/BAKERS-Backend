package com.example.bakersbackend.domain.crew.repository;

import com.example.bakersbackend.domain.crew.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    // 그룹 리스트
    List<Crew> findAllByOrderByCreatedAtDesc();

    // 각 크루별 총 거리 (running.distance 합계) - native 쿼리
    @Query(
            value = """
                SELECT r.crew_id AS crewId,
                       COALESCE(SUM(r.distance), 0) AS totalDistance
                FROM running r
                GROUP BY r.crew_id
                """,
            nativeQuery = true
    )
    List<CrewDistanceProjection> findCrewTotalDistances();

    // name LIKE 'keyword%' (대소문자 무시, 상위 10개)
    List<Crew> findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(String name);
}
