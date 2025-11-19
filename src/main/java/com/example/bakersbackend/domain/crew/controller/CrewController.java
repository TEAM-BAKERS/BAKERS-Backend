package com.example.bakersbackend.domain.crew.controller;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.crew.dto.CrewListResponse;
import com.example.bakersbackend.domain.crew.service.CrewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/group")
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
}
