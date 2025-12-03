package com.example.bakersbackend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /**
     * 시스템 기본 시계를 Bean으로 등록합니다.
     * 테스트에서는 고정된 시간의 Clock으로 대체할 수 있습니다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}