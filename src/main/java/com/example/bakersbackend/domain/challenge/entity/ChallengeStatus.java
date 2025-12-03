package com.example.bakersbackend.domain.challenge.entity;

public enum ChallengeStatus {
    ACTIVE,     // 진행 중
    SUCCESS,    // 목표 달성 성공
    FAILED      // 기간 만료 또는 실패
}