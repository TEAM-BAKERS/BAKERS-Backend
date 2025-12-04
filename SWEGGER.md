# Swagger/OpenAPI 설정 인수인계 문서

## 📋 목차
1. [개요](#개요)
2. [의존성 설정](#의존성-설정)
3. [SwaggerConfig 설정](#swaggerconfig-설정)
4. [application.yml 설정](#applicationyml-설정)
5. [Spring Security 설정](#spring-security-설정)
6. [컨트롤러에서 사용법](#컨트롤러에서-사용법)
7. [접속 방법](#접속-방법)
8. [다른 프로젝트에 적용하기](#다른-프로젝트에-적용하기)

---

## 개요

이 프로젝트는 **SpringDoc OpenAPI 3.0**을 사용하여 API 문서를 자동 생성합니다.

- **라이브러리**: SpringDoc OpenAPI (Springfox가 아님)
- **버전**: 2.8.9
- **Spring Boot 버전**: 3.4.4
- **Java 버전**: 17

---

## 의존성 설정

### build.gradle
파일 위치: `build.gradle`

```gradle
dependencies {
    // Swagger/OpenAPI 문서화
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.9'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9'
}
```

### 주요 의존성
- `springdoc-openapi-starter-webmvc-api`: OpenAPI 3.0 스펙 생성
- `springdoc-openapi-starter-webmvc-ui`: Swagger UI 제공

> **참고**: Spring Boot 3.x 이상에서는 `springdoc-openapi-starter-webmvc-*` 의존성을 사용해야 합니다.

---

## SwaggerConfig 설정

파일 위치: `src/main/java/com/dockersim/config/SwaggerConfig.java`

```java
package com.dockersim.config;

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

    private static final String HEADER_NAME = "X-User-Id";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DockerSim API Document")
                .description("Docker 명령어 학습을 위한 시뮬레이션 서비스의 API 명세서입니다.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Docker Simulation Team")
                    .email("yrkim6883@gmail.com"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("개발 서버")
            ))
            .components(new Components()
                .addSecuritySchemes(HEADER_NAME,
                    new SecurityScheme()
                        .name(HEADER_NAME)
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .description("개발용 사용자 Public ID 주입 헤더"))
            )
            .addSecurityItem(new SecurityRequirement().addList(HEADER_NAME));
    }
}
```

### 설정 설명

#### 1. API 정보 설정
```java
.info(new Info()
    .title("API 제목")
    .description("API 설명")
    .version("버전")
    .contact(new Contact()
        .name("팀명")
        .email("이메일"))
)
```

#### 2. 서버 설정
```java
.servers(List.of(
    new Server().url("서버 URL").description("서버 설명")
))
```
- 여러 환경(개발, 스테이징, 프로덕션)을 추가할 수 있습니다.

#### 3. 보안 스키마 설정
```java
.components(new Components()
    .addSecuritySchemes("헤더명",
        new SecurityScheme()
            .name("헤더명")
            .type(SecurityScheme.Type.APIKEY)  // APIKEY, HTTP, OAUTH2 등
            .in(SecurityScheme.In.HEADER)       // HEADER, QUERY, COOKIE 등
            .description("설명"))
)
.addSecurityItem(new SecurityRequirement().addList("헤더명"))
```

**보안 스키마 타입**:
- `APIKEY`: API 키 인증 (헤더, 쿼리 파라미터, 쿠키)
- `HTTP`: Basic, Bearer 등 HTTP 인증
- `OAUTH2`: OAuth 2.0
- `OPENIDCONNECT`: OpenID Connect

---

## application.yml 설정

파일 위치: `src/main/resources/application.yml`

```yaml
# Swagger/OpenAPI 설정
# http://localhost:8080/swagger-ui/index.html
springdoc:
  api-docs:
    enabled: true                    # API 문서 생성 활성화
    path: /v3/api-docs               # OpenAPI JSON 경로
  swagger-ui:
    enabled: true                    # Swagger UI 활성화
    path: /swagger-ui.html           # Swagger UI 접근 경로
    try-it-out-enabled: true         # Try it out 기능 활성화
    operations-sorter: alpha         # API 정렬 방식 (alpha: 알파벳순)
    tags-sorter: alpha               # 태그 정렬 방식
    display-request-duration: true   # 요청 소요 시간 표시
  show-actuator: false               # Spring Actuator 엔드포인트 숨김
```

### 주요 설정 항목

| 속성 | 설명 | 값 |
|-----|------|-----|
| `api-docs.enabled` | OpenAPI 문서 생성 활성화 | true/false |
| `api-docs.path` | OpenAPI JSON 경로 | 기본값: `/v3/api-docs` |
| `swagger-ui.enabled` | Swagger UI 활성화 | true/false |
| `swagger-ui.path` | Swagger UI 접근 경로 | 기본값: `/swagger-ui.html` |
| `swagger-ui.try-it-out-enabled` | Try it out 버튼 활성화 | true/false |
| `swagger-ui.operations-sorter` | API 정렬 방식 | alpha, method |
| `swagger-ui.tags-sorter` | 태그 정렬 방식 | alpha |
| `swagger-ui.display-request-duration` | 요청 소요 시간 표시 | true/false |

---

## Spring Security 설정

파일 위치: `src/main/java/com/dockersim/config/SecurityConfig.java`

Spring Security를 사용하는 경우, Swagger 관련 경로를 인증 없이 접근할 수 있도록 설정해야 합니다.

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/swagger-ui.html",       // Swagger UI HTML 페이지
                "/swagger-ui/**",         // Swagger UI 리소스
                "/v3/api-docs/**",        // API 문서
                "/swagger-resources/**",  // Swagger 리소스
                "/webjars/**"             // Swagger 의존성
            ).permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

### 필수 허용 경로
- `/swagger-ui.html`: Swagger UI 메인 페이지
- `/swagger-ui/**`: Swagger UI 정적 리소스 (CSS, JS 등)
- `/v3/api-docs/**`: OpenAPI JSON 문서
- `/swagger-resources/**`: Swagger 설정 리소스
- `/webjars/**`: WebJars 의존성 (UI 라이브러리)

---

## 컨트롤러에서 사용법

### 1. 컨트롤러 클래스 어노테이션

```java
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자 API", description = "사용자 생성, 조회, 삭제 관리 API")
public class UserController {
    // ...
}
```

### 2. API 메서드 어노테이션

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Operation(summary = "사용자 생성", description = "사용자를 생성합니다.")
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> createUser(
    @Parameter(description = "사용자 생성 요청 정보")
    @RequestBody UserRequest request
) {
    return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
}
```

### 3. 파라미터 숨김 처리

인증 정보 등 자동으로 주입되는 파라미터를 Swagger UI에서 숨기려면:

```java
@Operation(summary = "사용자 정보 조회", description = "사용자 정보를 조회합니다.")
@GetMapping
public ResponseEntity<ApiResponse<UserResponse>> getUser(
    @Parameter(hidden = true, description = "조회할 사용자 UUID")
    @AuthenticationPrincipal String userId
) {
    return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
}
```

### 4. 주요 어노테이션

| 어노테이션 | 위치 | 설명 |
|----------|------|------|
| `@Tag` | 클래스 | 컨트롤러 그룹화 및 설명 |
| `@Operation` | 메서드 | API 요약 및 상세 설명 |
| `@Parameter` | 파라미터 | 파라미터 설명 및 설정 |
| `@Schema` | DTO 필드 | 모델 필드 설명 |
| `@ApiResponse` | 메서드 | 응답 상태 코드 및 설명 |
| `@Hidden` | 클래스/메서드 | API 문서에서 숨김 |

### 5. DTO 문서화 예시

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 요청 정보")
public class UserRequest {

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    private String name;

    @Schema(description = "이메일 주소", example = "hong@example.com", required = true)
    private String email;
}
```

---

## 접속 방법

### Swagger UI 접속
```
http://localhost:8080/swagger-ui/index.html
```
또는
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON 문서
```
http://localhost:8080/v3/api-docs
```

### OpenAPI YAML 문서 (선택사항)
```
http://localhost:8080/v3/api-docs.yaml
```

---

## 다른 프로젝트에 적용하기

### 단계별 가이드

#### 1단계: 의존성 추가

**build.gradle**
```gradle
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.9'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9'
}
```

**pom.xml** (Maven 사용 시)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
    <version>2.8.9</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>
```

#### 2단계: SwaggerConfig 클래스 생성

`config` 패키지에 `SwaggerConfig.java` 생성:

```java
package com.yourproject.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                .title("프로젝트명 API")
                .description("프로젝트 설명")
                .version("1.0.0")
                .contact(new Contact()
                    .name("팀명")
                    .email("email@example.com"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("개발 서버")
            ));
    }
}
```

#### 3단계: application.yml 설정 추가

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: alpha
    tags-sorter: alpha
    display-request-duration: true
  show-actuator: false
```

#### 4단계: Security 설정 (Security 사용 시)

`SecurityConfig.java`에 Swagger 경로 허용 추가:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

#### 5단계: 컨트롤러에 어노테이션 추가

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/example")
@Tag(name = "예제 API", description = "예제 API 설명")
public class ExampleController {

    @Operation(summary = "목록 조회", description = "전체 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getList() {
        // ...
    }
}
```

#### 6단계: 접속 확인

애플리케이션 실행 후:
```
http://localhost:8080/swagger-ui/index.html
```

---

## 추가 설정 옵션

### JWT 인증 설정 예시

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-jwt",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization"))
        )
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
}
```

### 여러 서버 환경 설정

```java
.servers(List.of(
    new Server().url("http://localhost:8080").description("로컬 서버"),
    new Server().url("https://dev.example.com").description("개발 서버"),
    new Server().url("https://api.example.com").description("프로덕션 서버")
))
```

### 프로필별 Swagger 활성화/비활성화

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false  # 프로덕션에서는 비활성화
  swagger-ui:
    enabled: false
```

---

## 트러블슈팅

### 1. Swagger UI가 404 에러
- Security 설정에서 `/swagger-ui/**` 경로를 permitAll 했는지 확인
- `springdoc.swagger-ui.enabled: true` 설정 확인

### 2. API가 Swagger에 표시되지 않음
- 컨트롤러에 `@RestController` 또는 `@Controller` + `@ResponseBody` 확인
- `@Hidden` 어노테이션이 붙어있지 않은지 확인

### 3. 보안 스키마가 작동하지 않음
- `addSecurityItem(new SecurityRequirement().addList("스키마명"))` 추가 확인
- 스키마명이 `addSecuritySchemes`의 키와 일치하는지 확인

---

## 참고 자료

- [SpringDoc 공식 문서](https://springdoc.org/)
- [OpenAPI 3.0 스펙](https://swagger.io/specification/)
- [Swagger Annotations Guide](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

---

## 작성 정보

- **작성일**: 2025-11-30
- **프로젝트**: DockerSim Backend
- **버전**: Spring Boot 3.4.4, SpringDoc 2.8.9