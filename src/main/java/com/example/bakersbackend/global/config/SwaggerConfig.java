package com.example.bakersbackend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RunDuel API Document")
                .description("러닝 메이트 매칭 서비스 RunDuel의 API 명세서입니다.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("TEAM-BAKERS")
                    .email("team-bakers@example.com"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("로컬 서버"),
                new Server().url("https://13.124.218.49.nip.io").description("프로덕션 서버")
            ))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}