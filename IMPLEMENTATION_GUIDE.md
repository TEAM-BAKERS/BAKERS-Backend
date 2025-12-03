# 러닝 앱 대시보드 API 구현 가이드

> 러닝 기록, 크루 챌린지, 배틀 리그 기능 구현 완료

## 📋 목차

1. [구현 개요](#구현-개요)
2. [구현된 API 목록](#구현된-api-목록)
3. [디렉토리 구조](#디렉토리-구조)
4. [API 상세 명세](#api-상세-명세)
5. [비즈니스 로직](#비즈니스-로직)
6. [테스트 결과](#테스트-결과)
7. [사용 예시](#사용-예시)

---

## 구현 개요

### 구현 범위
- ✅ **핵심 엔진**: 러닝 기록 저장 및 챌린지/배틀 리그 자동 업데이트 (POST)
- ✅ **테스트 준비**: 챌린지 생성 (POST)
- ✅ **대시보드**: 챌린지 및 배틀 리그 현황 조회 (GET)

### 기술 스택
- **Backend**: Spring Boot 3.x, Java 17
- **Database**: H2 (test), MySQL 8.0 (production)
- **Concurrency**: Pessimistic Lock (PESSIMISTIC_WRITE)
- **Testing**: JUnit 5, Spring Boot Test

### 주요 특징
1. **동시성 제어**: 비관적 락으로 안전한 동시 업데이트 보장
2. **Fail-safe**: 진행 중인 챌린지/매치가 없어도 러닝 기록 정상 저장
3. **자동 상태 관리**: 목표 달성 시 챌린지 상태 자동 변경 (ACTIVE → SUCCESS)
4. **시간 주입**: Clock Bean을 사용한 테스트 가능한 시간 로직

---

## 구현된 API 목록

| 카테고리 | Method | Endpoint | 설명 |
|---------|--------|----------|------|
| **Running** | POST | `/api/v1/runnings` | 러닝 기록 저장 및 후처리 |
| **Challenge** | POST | `/api/v1/crews/{crewId}/challenges` | 크루 챌린지 생성 |
| **Challenge** | GET | `/api/v1/crews/{crewId}/challenges` | 크루 챌린지 목록 조회 |
| **Match** | GET | `/api/v1/matches/ongoing` | 진행 중인 배틀 리그 및 순위 조회 |

---

## 디렉토리 구조

```
src/main/java/com/example/bakersbackend/domain/
│
├── running/
│   ├── controller/
│   │   └── RunningController.java          # 러닝 기록 API
│   ├── dto/
│   │   ├── RunningRecordRequest.java       # 러닝 기록 요청 DTO
│   │   └── RunningRecordResponse.java      # 러닝 기록 응답 DTO
│   ├── entity/
│   │   └── Running.java                    # 러닝 엔티티
│   ├── repository/
│   │   └── RunningRepository.java
│   └── service/
│       └── RunningRecordService.java       # 러닝 기록 저장 + 후처리
│
├── challenge/
│   ├── controller/
│   │   └── ChallengeController.java        # 챌린지 API
│   ├── dto/
│   │   ├── CreateChallengeRequest.java     # 챌린지 생성 요청 DTO
│   │   └── ChallengeResponse.java          # 챌린지 응답 DTO
│   ├── entity/
│   │   ├── CrewChallenge.java              # 챌린지 엔티티
│   │   ├── CrewChallengeProgress.java      # 개인 기여도 엔티티
│   │   └── ChallengeStatus.java            # 챌린지 상태 ENUM
│   ├── repository/
│   │   ├── CrewChallengeRepository.java
│   │   └── CrewChallengeProgressRepository.java
│   └── service/
│       └── CrewChallengeService.java       # 챌린지 생성/조회/업데이트
│
└── match/
    ├── controller/
    │   └── MatchController.java            # 배틀 리그 API
    ├── dto/
    │   ├── MatchResponse.java              # 매치 정보 DTO
    │   ├── LeaderboardEntry.java           # 순위 항목 DTO
    │   └── OngoingMatchResponse.java       # 진행 중인 매치 응답 DTO
    ├── entity/
    │   ├── CrewMatch.java                  # 배틀 리그 엔티티
    │   └── CrewMatchParticipant.java       # 참가자 엔티티
    ├── repository/
    │   ├── CrewMatchRepository.java
    │   └── CrewMatchParticipantRepository.java
    └── service/
        └── CrewMatchService.java           # 배틀 리그 점수 업데이트/조회
```

---

## API 상세 명세

### 1️⃣ 러닝 기록 저장 API

**Endpoint**: `POST /api/v1/runnings`

**설명**: 러닝 기록을 저장하고 자동으로 챌린지 진행률과 배틀 리그 점수를 업데이트합니다.

**Request**:
```json
{
  "crewId": 1,
  "distance": 5000,
  "duration": 1800,
  "avgHeartrate": 145,
  "pace": 360
}
```

**Request Fields**:
| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| crewId | Long | ✅ | 크루 ID | - |
| distance | Integer | ✅ | 거리 (미터) | ≥ 1 |
| duration | Integer | ✅ | 시간 (초) | ≥ 1 |
| avgHeartrate | Short | ❌ | 평균 심박수 | - |
| pace | Integer | ❌ | 페이스 (초/1km) | - |

**Response**: `201 Created`
```json
{
  "runningId": 123,
  "userId": 5,
  "crewId": 1,
  "distance": 5000,
  "duration": 1800,
  "avgHeartrate": 145,
  "pace": 360,
  "startedAt": "2025-01-15T08:00:00",
  "createdAt": "2025-01-15T10:30:00"
}
```

**동작 흐름**:
1. 러닝 기록 DB 저장 (startedAt은 현재 시간으로 자동 설정)
2. 크루의 활성 챌린지가 있으면 진행률 업데이트
3. 진행 중인 배틀 리그가 있으면 점수 업데이트
4. 예외 발생 시에도 러닝 기록은 유지 (Fail-safe)

**Error Handling**:
- `404 Not Found`: 사용자 또는 크루를 찾을 수 없음
- `400 Bad Request`: 유효성 검증 실패

---

### 2️⃣ 챌린지 생성 API

**Endpoint**: `POST /api/v1/crews/{crewId}/challenges`

**설명**: 크루 챌린지를 생성합니다.

**Path Parameters**:
| 필드 | 타입 | 설명 |
|------|------|------|
| crewId | Long | 크루 ID |

**Request**:
```json
{
  "title": "100km 완주 챌린지",
  "description": "크루 전체가 100km를 달려봅시다!",
  "goalDistance": 100000,
  "endDate": "2025-01-31T23:59:59"
}
```

**Request Fields**:
| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| title | String | ✅ | 챌린지 제목 | - |
| description | String | ❌ | 챌린지 설명 | - |
| goalDistance | Integer | ✅ | 목표 거리 (미터) | ≥ 1000 |
| endDate | LocalDateTime | ✅ | 종료 날짜 | 미래 날짜 |

**Response**: `201 Created`
```json
{
  "challengeId": 10,
  "title": "100km 완주 챌린지",
  "description": "크루 전체가 100km를 달려봅시다!",
  "goalDistance": 100000,
  "currentAccumulatedDistance": 0,
  "status": "ACTIVE",
  "progressPercentage": 0.0,
  "startAt": "2025-01-15T10:00:00",
  "endAt": "2025-01-31T23:59:59"
}
```

**자동 설정**:
- `startAt`: 생성 시점으로 자동 설정
- `currentAccumulatedDistance`: 0으로 초기화
- `status`: ACTIVE로 설정

---

### 3️⃣ 챌린지 목록 조회 API

**Endpoint**: `GET /api/v1/crews/{crewId}/challenges`

**설명**: 크루의 모든 챌린지 목록을 최신순으로 조회합니다.

**Path Parameters**:
| 필드 | 타입 | 설명 |
|------|------|------|
| crewId | Long | 크루 ID |

**Response**: `200 OK`
```json
[
  {
    "challengeId": 10,
    "title": "100km 완주 챌린지",
    "description": "크루 전체가 100km를 달려봅시다!",
    "goalDistance": 100000,
    "currentAccumulatedDistance": 45000,
    "status": "ACTIVE",
    "progressPercentage": 45.0,
    "startAt": "2025-01-01T00:00:00",
    "endAt": "2025-01-31T23:59:59"
  },
  {
    "challengeId": 9,
    "title": "12월 마라톤",
    "description": null,
    "goalDistance": 42195,
    "currentAccumulatedDistance": 42195,
    "status": "SUCCESS",
    "progressPercentage": 100.0,
    "startAt": "2024-12-01T00:00:00",
    "endAt": "2024-12-31T23:59:59"
  }
]
```

**응답 특징**:
- 최신 생성순 정렬 (createdAt DESC)
- `progressPercentage`는 자동 계산 (최대 100%)
- 모든 상태(ACTIVE, SUCCESS, FAILED) 포함

---

### 4️⃣ 진행 중인 배틀 리그 조회 API

**Endpoint**: `GET /api/v1/matches/ongoing`

**설명**: 현재 진행 중인 배틀 리그와 크루별 순위를 조회합니다.

**Response**: `200 OK`
```json
{
  "match": {
    "matchId": 5,
    "title": "1월 배틀 리그",
    "description": "1월 한 달간 진행되는 크루 대항전",
    "startAt": "2025-01-01T00:00:00",
    "endAt": "2025-01-31T23:59:59"
  },
  "leaderboard": [
    {
      "rank": 1,
      "crewId": 3,
      "crewName": "달리기 크루",
      "totalDistance": 250000
    },
    {
      "rank": 2,
      "crewId": 7,
      "crewName": "러닝 메이트",
      "totalDistance": 230000
    },
    {
      "rank": 3,
      "crewId": 1,
      "crewName": "테스트 크루",
      "totalDistance": 180000
    }
  ]
}
```

**진행 중인 매치가 없을 경우**: `204 No Content`

**순위 기준**:
- `totalDistance` 내림차순 정렬
- 동점일 경우 순위는 동일 (rank는 순차적으로 증가)

---

## 비즈니스 로직

### 챌린지 진행률 업데이트

**트리거**: 러닝 기록 저장 시 자동 실행

**로직**:
1. 크루의 ACTIVE 상태 챌린지 조회 (비관적 락)
2. 챌린지 전체 누적 거리 증가 (`currentAccumulatedDistance += distance`)
3. 개인 기여도 업데이트 또는 신규 생성 (`CrewChallengeProgress`)
4. 목표 달성 체크:
   - `currentAccumulatedDistance >= goalDistance` → `status = SUCCESS`

**동시성 제어**:
- `PESSIMISTIC_WRITE` 락 (타임아웃: 3초)
- Unique Constraint 위반 시 재조회 후 업데이트

**Empty Handling**:
- 활성 챌린지가 없으면 로그만 출력하고 종료 (에러 없음)

---

### 배틀 리그 점수 업데이트

**트리거**: 러닝 기록 저장 시 자동 실행

**로직**:
1. 현재 시간 기준 진행 중인 매치 조회
   - `now BETWEEN match.startAt AND match.endAt`
2. 크루의 참가자 정보 조회 (비관적 락)
3. 총 거리 증가 (`totalDistance += distance`)
4. 참가자 정보가 없으면 신규 생성 (자동 참가)

**동시성 제어**:
- `PESSIMISTIC_WRITE` 락 (타임아웃: 3초)
- Unique Constraint 위반 시 재조회 후 업데이트

**Empty Handling**:
- 진행 중인 매치가 없으면 로그만 출력하고 종료 (에러 없음)

---

### 시간 관리 (Clock Injection)

**목적**: 테스트 가능한 시간 로직 구현

**설정**:
```java
// src/main/java/...global/config/ClockConfig.java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone(); // Production
}

// 테스트에서 고정 시간 사용 가능
@Bean
@Primary
public Clock testClock() {
    LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 15, 12, 0, 0);
    return Clock.fixed(fixedTime.atZone(ZoneId.systemDefault()).toInstant(),
                       ZoneId.systemDefault());
}
```

**사용 위치**:
- 러닝 기록의 `startedAt` 설정
- 챌린지의 `startAt` 설정
- 진행 중인 매치 조회 시 현재 시간 판단

---

## 테스트 결과

### 테스트 구성
총 **19개 테스트** 모두 PASS ✅

#### 1. RunningRecordServiceTest (7개)
- ✅ 러닝 기록 저장 시 챌린지 및 배틀 리그 자동 갱신
- ✅ 동시성 테스트: 10개 스레드가 동시에 기록해도 데이터 무결성 보장 (30km 정확히 누적)
- ✅ 챌린지 목표 달성 시 상태 SUCCESS로 변경
- ✅ 진행 중인 챌린지 없을 때 에러 없이 러닝 기록만 저장
- ✅ 진행 중인 배틀 리그 없을 때 에러 없이 러닝 기록만 저장
- ✅ 동일 유저 여러 번 러닝 시 개인 기여도 누적
- ✅ 전체 플로우 검증

#### 2. CrewChallengeServiceTest (6개)
- ✅ 활성 챌린지 없을 때 업데이트하지 않음
- ✅ 챌린지 진행률 업데이트 (크루 전체 거리 + 개인 기여도)
- ✅ 여러 번 기여 시 누적
- ✅ 목표 거리 달성 시 상태 변경 (ACTIVE → SUCCESS)
- ✅ 목표 초과 달성 시에도 SUCCESS 처리
- ✅ 여러 유저 동시 기여 시 데이터 무결성 유지

#### 3. CrewMatchServiceTest (6개)
- ✅ 진행 중인 매치 없을 때 업데이트하지 않음
- ✅ 배틀 리그 점수 업데이트
- ✅ 여러 번 점수 누적
- ✅ 여러 크루 동시 참가 시 각각 독립적으로 관리
- ✅ 매치 기간 외에는 업데이트되지 않음
- ✅ 여러 매치 중 현재 진행 중인 매치만 업데이트

### 빌드 결과
```
BUILD SUCCESSFUL in 24s
5 actionable tasks: 3 executed, 2 up-to-date
```

---

## 사용 예시

### 시나리오: 크루원들이 함께 100km 챌린지 달성

#### Step 1: 챌린지 생성
```bash
POST /api/v1/crews/1/challenges
{
  "title": "100km 완주 챌린지",
  "goalDistance": 100000,
  "endDate": "2025-02-28T23:59:59"
}
```

#### Step 2: 크루원 A가 러닝 기록
```bash
POST /api/v1/runnings
{
  "crewId": 1,
  "distance": 5000,
  "duration": 1800
}
```
→ 챌린지 진행률: 5km / 100km (5%)

#### Step 3: 크루원 B가 러닝 기록
```bash
POST /api/v1/runnings
{
  "crewId": 1,
  "distance": 10000,
  "duration": 3600
}
```
→ 챌린지 진행률: 15km / 100km (15%)

#### Step 4: 챌린지 현황 조회
```bash
GET /api/v1/crews/1/challenges
```
Response:
```json
[
  {
    "challengeId": 1,
    "title": "100km 완주 챌린지",
    "currentAccumulatedDistance": 15000,
    "progressPercentage": 15.0,
    "status": "ACTIVE"
  }
]
```

#### Step 5: 목표 달성 (95km → 100km)
```bash
POST /api/v1/runnings
{
  "crewId": 1,
  "distance": 5000,
  "duration": 1800
}
```
→ 챌린지 상태 자동 변경: `ACTIVE` → `SUCCESS` 🎉

#### Step 6: 배틀 리그 순위 확인
```bash
GET /api/v1/matches/ongoing
```
Response:
```json
{
  "match": {
    "matchId": 1,
    "title": "2월 배틀 리그"
  },
  "leaderboard": [
    {
      "rank": 1,
      "crewId": 1,
      "crewName": "우리 크루",
      "totalDistance": 100000
    }
  ]
}
```

---

## 인증 및 권한

### 인증 방식
- **JWT Bearer Token** 사용
- Header: `Authorization: Bearer {accessToken}`

### 현재 구현된 인증
- `@AuthenticationPrincipal User` 파라미터로 인증된 사용자 정보 자동 주입
- 인증되지 않은 사용자는 401 Unauthorized 응답

### 향후 권한 규칙 (제안)
- 러닝 기록 생성: 본인만 가능
- 챌린지 조회: 크루 멤버만 가능
- 챌린지 생성: 크루 오너만 가능
- 배틀 리그 조회: 모든 인증된 사용자

---

## 에러 코드

| HTTP Status | 설명 |
|-------------|------|
| 200 OK | 정상 조회 |
| 201 Created | 리소스 생성 성공 |
| 204 No Content | 진행 중인 매치 없음 |
| 400 Bad Request | 유효성 검증 실패 |
| 401 Unauthorized | 인증 토큰 없음 또는 만료 |
| 404 Not Found | 리소스를 찾을 수 없음 |

---

## 다음 단계 (제안)

### 1. 추가 API 구현
- [ ] `GET /api/v1/crews/{crewId}/challenges/{challengeId}/leaderboard` - 챌린지 개인 기여도 순위
- [ ] `GET /api/v1/crews/{crewId}/challenges/{challengeId}/my-progress` - 내 개인 기여도 조회
- [ ] `POST /api/v1/matches` - 배틀 리그 생성 (관리자용)

### 2. 스케줄러 추가
- [ ] 종료된 챌린지 상태 변경 (ACTIVE → FAILED)
- [ ] 배틀 리그 자동 시작/종료

### 3. 성능 최적화
- [ ] 배틀 리그 순위 캐싱 (Redis, 1분)
- [ ] 챌린지 진행률 N+1 문제 해결

### 4. 보안 강화
- [ ] 크루 멤버 권한 검증
- [ ] Rate Limiting 추가

---
- [테스트 코드 위치](./src/test/java/com/example/bakersbackend/domain/)