package com.example.bakersbackend.domain.match.service;

import com.example.bakersbackend.domain.crew.entity.Crew;
import com.example.bakersbackend.domain.match.entity.CrewMatch;
import com.example.bakersbackend.domain.match.entity.CrewMatchParticipant;
import com.example.bakersbackend.domain.match.repository.CrewMatchParticipantRepository;
import com.example.bakersbackend.domain.match.repository.CrewMatchRepository;
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
public class CrewMatchService {

    private final CrewMatchRepository crewMatchRepository;
    private final CrewMatchParticipantRepository crewMatchParticipantRepository;
    private final Clock clock;

    /**
     * 러닝 기록 후 배틀 리그 점수를 갱신합니다.
     * 동시성 제어를 위해 비관적 락을 사용합니다.
     */
    @Transactional
    public void updateMatchScore(Crew crew, Integer distance) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. 현재 진행 중인 매치 조회
        Optional<CrewMatch> matchOpt = crewMatchRepository.findOngoingMatch(now);

        // 2. Empty Handling: 진행 중인 매치가 없으면 종료
        if (matchOpt.isEmpty()) {
            log.debug("현재 진행 중인 배틀 리그가 없습니다. 점수 업데이트를 건너뜁니다.");
            return;
        }

        CrewMatch match = matchOpt.get();

        // 3. 참가자 조회 및 점수 업데이트
        updateParticipantScore(match, crew, distance);
    }

    /**
     * 크루의 배틀 리그 참가자 점수를 업데이트합니다.
     * Unique Constraint 위반 시 재조회하여 안전하게 처리합니다.
     */
    private void updateParticipantScore(CrewMatch match, Crew crew, Integer distance) {
        try {
            // 비관적 락으로 참가자 조회
            Optional<CrewMatchParticipant> participantOpt =
                    crewMatchParticipantRepository.findByMatchAndCrew(match, crew);

            if (participantOpt.isPresent()) {
                // 기존 참가자가 있으면 거리 증가
                CrewMatchParticipant participant = participantOpt.get();
                participant.addDistance(distance);
                log.debug("배틀 리그 점수 업데이트: matchId={}, crewId={}, 추가거리={}m, 총거리={}m",
                        match.getId(), crew.getId(), distance, participant.getTotalDistance());
            } else {
                // 참가자가 없으면 새로 생성
                CrewMatchParticipant newParticipant = CrewMatchParticipant.createNew(match, crew, distance);
                crewMatchParticipantRepository.save(newParticipant);
                log.info("배틀 리그 신규 참가: matchId={}, crewId={}, 초기거리={}m",
                        match.getId(), crew.getId(), distance);
            }
        } catch (DataIntegrityViolationException e) {
            // Unique Constraint 위반: 동시 삽입으로 인한 경합 발생
            // 재조회하여 업데이트
            log.debug("배틀 리그 참가자 동시 삽입 감지. 재조회 후 업데이트합니다. matchId={}, crewId={}",
                    match.getId(), crew.getId());

            CrewMatchParticipant existingParticipant = crewMatchParticipantRepository
                    .findByMatchAndCrew(match, crew)
                    .orElseThrow(() -> new IllegalStateException(
                            "배틀 리그 참가자 재조회 실패. matchId=" + match.getId() + ", crewId=" + crew.getId()));

            existingParticipant.addDistance(distance);
        }
    }

    /**
     * 현재 진행 중인 배틀 리그를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<CrewMatch> getOngoingMatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        return crewMatchRepository.findOngoingMatch(now);
    }

    /**
     * 특정 매치의 참가자 순위를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewMatchParticipant> getMatchLeaderboard(CrewMatch match) {
        return crewMatchParticipantRepository.findByMatchOrderByTotalDistanceDesc(match);
    }
}