package com.example.bakersbackend.domain.auth.controller;

import com.example.bakersbackend.domain.auth.dto.*;
import com.example.bakersbackend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest req) {
        SignUpResponse res = authService.signUp(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(res);
    }

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest req) {
        SignInResponse res = authService.signIn(req);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(res);
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<SignInResponse> refreshAccessToken(@Valid @RequestBody RefreshTokenRequest req) {
        SignInResponse res = authService.reissueAccessToken(req);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(res);
    }
}
