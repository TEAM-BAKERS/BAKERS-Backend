package com.example.bakersbackend.domain.auth.service;

import com.example.bakersbackend.domain.auth.dto.SignUpRequest;
import com.example.bakersbackend.domain.auth.dto.SignUpResponse;
import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
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

}
