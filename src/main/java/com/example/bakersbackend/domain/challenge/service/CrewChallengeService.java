package com.example.bakersbackend.domain.challenge.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.challenge.entity.ChallengeStatus;
import com.example.bakersbackend.domain.challenge.entity.CrewChallenge;
import com.example.bakersbackend.domain.challenge.entity.CrewChallengeProgress;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeProgressRepository;
import com.example.bakersbackend.domain.challenge.repository.CrewChallengeRepository;
import com.example.bakersbackend.domain.crew.entity.Crew;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrewChallengeService {

    private final CrewChallengeRepository crewChallengeRepository;
    private final CrewChallengeProgressRepository crewChallengeProgressRepository;
    private final Clock clock;

    /**
     * 러닝 기록 후 챌린지 진행률을 갱신합니다.
     * 동시성 제어를 위해 비관적 락을 사용합니다.
     */
    @Transactional
    public void updateChallengeProgress(Crew crew, User user, Integer distance) {
        // 1. 활성 챌린지 조회 (비관적 락)
        Optional<CrewChallenge> challengeOpt = crewChallengeRepository
                .findActiveChallengeByCrewWithLock(crew, ChallengeStatus.ACTIVE);

        // 2. Empty Handling: 활성 챌린지가 없으면 종료
        if (challengeOpt.isEmpty()) {
            log.debug("크루 {}에 진행 중인 챌린지가 없습니다. 챌린지 업데이트를 건너뜁니다.", crew.getId());
            return;
        }

        CrewChallenge challenge = challengeOpt.get();

        // 3. 크루 전체 누적 거리 증가 (비관적 락으로 안전하게 업데이트)
        challenge.addAccumulatedDistance(distance);

        // 4. 개인 기여도 업데이트
        updateUserContribution(challenge, user, distance);

        // 5. 목표 달성 체크
        if (challenge.checkAndMarkSuccessIfGoalReached()) {
            log.info("🎉 축하합니다! 크루 {} 챌린지 '{}'가 목표를 달성했습니다! (목표: {}m, 달성: {}m)",
                    crew.getId(),
                    challenge.getTitle(),
                    challenge.getGoalValue(),
                    challenge.getCurrentAccumulatedDistance());
        }
    }

    /**
     * 유저의 개인 기여도를 업데이트합니다.
     * Unique Constraint 위반 시 재조회하여 안전하게 처리합니다.
     */
    private void updateUserContribution(CrewChallenge challenge, User user, Integer distance) {
        try {
            Optional<CrewChallengeProgress> progressOpt =
                    crewChallengeProgressRepository.findByChallengeAndUser(challenge, user);

            if (progressOpt.isPresent()) {
                // 기존 진행률이 있으면 거리 증가
                CrewChallengeProgress progress = progressOpt.get();
                progress.addDistance(distance);
            } else {
                // 진행률이 없으면 새로 생성
                CrewChallengeProgress newProgress = CrewChallengeProgress.createNew(challenge, user, distance);
                crewChallengeProgressRepository.save(newProgress);
            }
        } catch (DataIntegrityViolationException e) {
            // Unique Constraint 위반: 동시 삽입으로 인한 경합 발생
            // 재조회하여 업데이트
            log.debug("챌린지 진행률 동시 삽입 감지. 재조회 후 업데이트합니다. challengeId={}, userId={}",
                    challenge.getId(), user.getId());

            CrewChallengeProgress existingProgress = crewChallengeProgressRepository
                    .findByChallengeAndUser(challenge, user)
                    .orElseThrow(() -> new IllegalStateException(
                            "챌린지 진행률 재조회 실패. challengeId=" + challenge.getId() + ", userId=" + user.getId()));

            existingProgress.addDistance(distance);
        }
    }

    /**
     * 크루 챌린지를 생성합니다.
     */
    @Transactional
    public CrewChallenge createChallenge(Crew crew, String title, String description, Integer goalValue, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now(clock);

        CrewChallenge challenge = CrewChallenge.builder()
                .crew(crew)
                .title(title)
                .description(description)
                .goalValue(goalValue)
                .currentAccumulatedDistance(0)
                .status(ChallengeStatus.ACTIVE)
                .startAt(now)
                .endAt(endDate)
                .build();

        CrewChallenge savedChallenge = crewChallengeRepository.save(challenge);
        log.info("크루 {} 챌린지 생성: '{}'(목표: {}m, 종료일: {})",
                crew.getId(), title, goalValue, endDate);

        return savedChallenge;
    }

    /**
     * 크루의 활성 챌린지를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<CrewChallenge> getActiveChallenge(Crew crew) {
        return crewChallengeRepository.findByCrewAndStatus(crew, ChallengeStatus.ACTIVE);
    }

    /**
     * 크루의 모든 챌린지를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewChallenge> getAllChallenges(Crew crew) {
        return crewChallengeRepository.findByCrewOrderByCreatedAtDesc(crew);
    }
}