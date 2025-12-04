package com.example.bakersbackend.domain.match.dto;

public record MemberContribution(
        Long userId,
        String nickname,
        Integer distance,
        Integer rank
) {
    public static MemberContribution of(Long userId, String nickname, Integer distance, Integer rank) {
        return new MemberContribution(userId, nickname, distance, rank);
    }
}
