package com.example.bakersbackend.domain.home.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.home.dto.HomeResponse;
import com.example.bakersbackend.domain.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈 화면 API", description = "메인 홈 화면 정보 조회 API")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 조회", description = "배틀 리그 간략 정보, 오늘의 러닝 기록, 크루 최근 활동을 조회합니다.")
    @GetMapping
    public ResponseEntity<HomeResponse> getHome(
            @AuthenticationPrincipal User user
    ) {
        HomeResponse response = homeService.getHome(user);
        return ResponseEntity.ok(response);
    }
}
