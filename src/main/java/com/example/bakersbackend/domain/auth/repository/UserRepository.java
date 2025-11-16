package com.example.bakersbackend.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.bakersbackend.domain.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 중복 체크 시 사용
    boolean existsByEmail(String email);

    // 로그인 시 사용
    Optional<User> findByEmail(String email);
}
