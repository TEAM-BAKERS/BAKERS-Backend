package com.example.bakersbackend.domain.auth.service;

import com.example.bakersbackend.domain.auth.dto.SignInRequest;
import com.example.bakersbackend.domain.auth.dto.SignInResponse;
import com.example.bakersbackend.domain.auth.dto.SignUpRequest;
import com.example.bakersbackend.domain.auth.dto.SignUpResponse;
import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import com.example.bakersbackend.global.jwt.JwtProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public SignUpResponse signUp(SignUpRequest req) {
        // 사용 중인 이메일이 있다면
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .currentGroupId(null)
                .refreshToken(null)
                .build();


        User savedUser = userRepository.save(user);

        return new SignUpResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    public SignInResponse signIn(@Valid SignInRequest req) {

        // 이메일로 유저 조회
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        // 비미럽호 검증
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        // 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        // DB 저장
        user.changeRefreshToken(refreshToken);

        return new SignInResponse(accessToken, refreshToken, "Bearer", 1800);
    }
}
