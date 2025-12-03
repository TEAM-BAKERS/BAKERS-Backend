package com.example.bakersbackend.domain.mypage.service;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.domain.mypage.dto.MypageSummaryResponse;
import com.example.bakersbackend.domain.running.repository.RunningRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final RunningRepository runningRepository;

    // 마이페이지 데이터
    public MypageSummaryResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없음. id=" + userId));

        // 변수 초기화
        Long totalDistanceMeters = runningRepository.sumDistanceByUserId(userId);
        Long totalDurationSec = runningRepository.sumDurationByUserId(userId);
        Long runningCount = runningRepository.countByUserId(userId);

        if (totalDistanceMeters == null) totalDistanceMeters = 0L;
        if (totalDurationSec == null) totalDurationSec = 0L;
        if (runningCount == null) runningCount = 0L;

        // 총 뛴 거리를 km로 반환
        double totalDistanceKm = 0.0;
        if (totalDistanceMeters > 0) {
            totalDistanceKm = totalDistanceMeters / 1000.0;
            totalDistanceKm = Math.round(totalDistanceKm * 10) / 10.0;
        }

        // 1km 뛰는 데 얼마나 걸리는지 계산
        String averagePace = "-";
        if (totalDistanceMeters > 0 && totalDurationSec > 0) {
            double paceSecPerKm = totalDurationSec / (totalDistanceMeters / 1000.0);
            int paceSecInt = (int) Math.round(paceSecPerKm);

            int minutes = paceSecInt / 60;
            int seconds = paceSecInt % 60;

            averagePace = String.format("%d'%02d\"", minutes, seconds);
        }

        // 가입한 날짜
        String joinDate = null;
        if (user.getCreatedAt() != null) {
            LocalDate joinLocalDate = user.getCreatedAt().toLocalDate();
            joinDate = joinLocalDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }

        return new MypageSummaryResponse(
                user.getNickname(),
                user.getImageUrl(),
                joinDate,
                totalDistanceKm,
                runningCount,
                averagePace
        );
    }
}
