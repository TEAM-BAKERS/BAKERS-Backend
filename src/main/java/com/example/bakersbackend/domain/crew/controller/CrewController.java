package com.example.bakersbackend.domain.crew.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.dto.CrewCreateRequest;
import com.example.bakersbackend.domain.crew.dto.CrewListResponse;
import com.example.bakersbackend.domain.crew.dto.CrewSearchResponse;
import com.example.bakersbackend.domain.crew.dto.CrewSignUpRequest;
import com.example.bakersbackend.domain.crew.service.CrewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    // 그룹 목록 조회
    @GetMapping("/list")
    public CrewListResponse getGroupList(
            @AuthenticationPrincipal User user
    ) {
        return crewService.getAllGroups();
    }

    // 그룹 가입
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            @AuthenticationPrincipal User user,
            @RequestBody CrewSignUpRequest request
    ) {
        Long crewId = request.crewId();
        return crewService.signUpGroups(crewId, user.getId());
    }

    // 검색어 자동완성
    @GetMapping("/autocomplete")
    public List<CrewSearchResponse> searchAuto(
            @RequestParam String keyword
    ){
        return crewService.autocomplete(keyword);
    }

    // 크루 생성
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CrewCreateRequest request
    ) throws IOException {

        Map<String, Object> result = crewService.saveCrew(user.getId(), request);
        return ResponseEntity.ok(result);
    }
}
