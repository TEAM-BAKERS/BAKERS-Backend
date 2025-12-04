package com.example.bakersbackend.domain.crew.entity;

public enum MemberStatus {
    PENDING,   // 가입 대기
    APPROVED,  // 승인됨
    REJECTED,  // 거절됨
    BLOCKED    // 차단 등 필요 시
}