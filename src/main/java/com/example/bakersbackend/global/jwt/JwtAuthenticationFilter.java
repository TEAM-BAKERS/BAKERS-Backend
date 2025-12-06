package com.example.bakersbackend.global.jwt;

import com.example.bakersbackend.domain.auth.entity.User;
import com.example.bakersbackend.domain.auth.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // 헤더 없으면 다음 필터로
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Authorization 헤더 없음 또는 Bearer 형식 아님");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.debug("토큰 추출 성공: 길이={}", token.length());

        try {
            boolean valid = jwtProvider.validateToken(token, JwtType.ACCESS);
            if (!valid) {
                // TODO : 응답 지우기
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("JWT_ERROR: invalid token");

                log.warn("토큰 검증 실패");
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtProvider.getUserId(token, JwtType.ACCESS);
            log.debug("토큰에서 userId 추출: {}", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 권한은 아직 안쓰니까 비워뒀습니다.
            var authentication = new UsernamePasswordAuthenticationToken(
                    user,                      // principal
                    null,                      // credentials
                    Collections.emptyList()    // authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("인증 성공: userId={}, email={}", user.getId(), user.getEmail());

        } catch (JwtException | IllegalArgumentException e) {
            // TODO : 응답 지우기
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("JWT_ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());

            log.error("JWT 인증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
