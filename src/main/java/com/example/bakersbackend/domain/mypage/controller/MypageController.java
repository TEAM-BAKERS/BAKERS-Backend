package com.example.bakersbackend.domain.mypage.controller;


import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.mypage.dto.MypageSummaryResponse;
import com.example.bakersbackend.domain.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {
    private final MypageService myPageService;

    // 마이페이지 접속
    @GetMapping("")
    public ResponseEntity<MypageSummaryResponse> getMyPage(
            @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        MypageSummaryResponse response = myPageService.getMyPage(userId);
        return ResponseEntity.ok(response);
    }
}
