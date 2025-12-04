package com.example.bakersbackend.domain.auth.controller;

import com.example.bakersbackend.domain.auth.dto.MeResponse;
import com.example.bakersbackend.domain.auth.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserMeController {

    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        // JwtAuthenticationFilter 에서 넣어준 principal
        var user = (User) authentication.getPrincipal();
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
