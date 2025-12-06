package com.example.bakersbackend.domain.challenge.initializer;

import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.ChallengeType;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.crew.repository.CrewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "dev", "test"})
public class ChallengeDataInitializer implements CommandLineRunner {

    private final CrewRepository crewRepository;
    private final CrewChallengeRepository crewChallengeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== 크루 챌린지 초기화 시작 ===");

        List<Crew> crews = crewRepository.findAll();

        if (crews.isEmpty()) {
            log.info("크루가 없습니다. 챌린지 초기화를 건너뜁니다.");
            return;
        }

        int createdCount = 0;
        int updatedCount = 0;

        for (Crew crew : crews) {
            int[] counts = initializeCrewChallenges(crew);
            createdCount += counts[0];
            updatedCount += counts[1];
        }

        log.info("=== 크루 챌린지 초기화 완료 (생성: {}, 업데이트: {}) ===", createdCount, updatedCount);
    }

    private int[] initializeCrewChallenges(Crew crew) {
        int created = 0;
        int updated = 0;

        // 12월 챌린지 1개만 생성
        if (createMonthlyChallenge(
                crew,
                12,
                "연말 100km 챌린지",
                "2025년을 마무리하며 크루 전체가 100km 완주하기",
                100000
        )) {
            created++;
        }

        return new int[]{created, updated};
    }

    /**
     * 월별 챌린지를 생성합니다. 이미 존재하면 생성하지 않습니다.
     *
     * @return true: 새로 생성됨, false: 이미 존재함
     */
    private boolean createMonthlyChallenge(
            Crew crew,
            int month,
            String title,
            String description,
            Integer goalValue
    ) {
        LocalDateTime startAt = LocalDateTime.of(2025, month, 1, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2025, month, 1, 0, 0)
                .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                .withHour(23)
                .withMinute(59)
                .withSecond(59);

        // 같은 기간의 챌린지가 이미 있는지 확인
        Optional<CrewChallenge> existing =
                crewChallengeRepository.findByCrewAndStartAtAndEndAt(crew, startAt, endAt);

        if (existing.isPresent()) {
            log.debug("크루 {}의 {}월 챌린지가 이미 존재합니다.", crew.getId(), month);
            return false;
        }

        // 새 챌린지 생성
        CrewChallenge newChallenge = CrewChallenge.builder()
                .crew(crew)
                .title(title)
                .description(description)
                .type(ChallengeType.DISTANCE)
                .goalValue(goalValue)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(startAt)
                .endAt(endAt)
                .build();

        crewChallengeRepository.save(newChallenge);
        log.info("크루 {}의 {}월 챌린지 생성: '{}'", crew.getId(), month, title);
        return true;
    }
}
