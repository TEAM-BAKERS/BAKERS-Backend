# BAKERS Backend API 명세서

## 목차
1. [인증 API](#1-인증-api)
2. [사용자 API](#2-사용자-api)
3. [홈 화면 API](#3-홈-화면-api)
4. [크루 API](#4-크루-api)
5. [러닝 기록 API](#5-러닝-기록-api)
6. [챌린지 API](#6-챌린지-api)
7. [배틀 리그 API](#7-배틀-리그-api)
8. [마이페이지 API](#8-마이페이지-api)

---

## 1. 인증 API

### 1.1 회원가입
```
POST /auth/signup
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

**Validation**
- `email`: 필수, 이메일 형식
- `password`: 필수, 6-100자
- `nickname`: 필수, 2-20자

**Response** `201 Created`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.2 로그인
```
POST /auth/signin
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.3 액세스 토큰 갱신
```
POST /auth/token/refresh
```

**Request Body**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** `200 OK`
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## 2. 사용자 API

### 2.1 내 정보 조회
```
GET /api/me
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동"
}
```

---

## 3. 홈 화면 API

### 3.1 홈 화면 조회
```
GET /api/v1/home
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "battleLeague": {
    "myCrewName": "아침 러너스",
    "opponentCrewName": "저녁 산책",
    "myCrewDistance": 250000,
    "opponentCrewDistance": 180000
  },
  "todayRunning": {
    "distance": 5000,
    "duration": 1800,
    "pace": 360
  },
  "recentActivities": [
    {
      "nickname": "홍길동",
      "distance": 5000,
      "duration": 1800,
      "pace": 360
    },
    {
      "nickname": "김철수",
      "distance": 3000,
      "duration": 1200,
      "pace": 400
    },
    {
      "nickname": "이영희",
      "distance": 7000,
      "duration": 2400,
      "pace": 343
    },
    {
      "nickname": "박민수",
      "distance": 4000,
      "duration": 1600,
      "pace": 400
    },
    {
      "nickname": "정수진",
      "distance": 6000,
      "duration": 2100,
      "pace": 350
    }
  ]
}
```

**데이터가 없는 경우** `200 OK`
```json
{
  "battleLeague": null,
  "todayRunning": null,
  "recentActivities": []
}
```

**설명**
- `battleLeague`: 진행 중인 배틀 리그가 없거나 내 크루가 참가하지 않은 경우 `null`
- `todayRunning`: 오늘 러닝 기록이 없는 경우 `null` (가장 최근 1개만 반환)
- `recentActivities`: 크루의 최근 러닝 기록 최대 5개 (없으면 빈 배열)

---

## 4. 크루 API

### 4.1 크루 목록 조회
```
GET /api/crew/list
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "crews": [
    {
      "id": 1,
      "name": "아침 러너스",
      "intro": "아침에 달리는 크루입니다",
      "memberCount": 15,
      "tags": [
        {
          "id": 1,
          "name": "초보환영"
        },
        {
          "id": 2,
          "name": "주3회"
        }
      ]
    }
  ]
}
```

---

### 4.2 크루 검색 자동완성
```
GET /api/crew/autocomplete?keyword={keyword}
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Query Parameters**
- `keyword`: 검색 키워드 (필수)

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "name": "아침 러너스",
    "intro": "아침에 달리는 크루입니다"
  },
  {
    "id": 2,
    "name": "아침 산책",
    "intro": "산책하는 크루~"
  }
]
```

---

### 4.3 크루 생성
```
POST /api/crew
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "name": "새로운 크루",
  "intro": "크루 소개",
  "imgUrl": "https://example.com/image.jpg",
  "tags": [
    {
      "id": 1,
      "name": "초보환영"
    }
  ]
}
```

**Validation**
- `name`: 필수

**Response** `200 OK`
```json
{
  "crewId": 1,
  "name": "새로운 크루",
  "message": "크루가 생성되었습니다"
}
```

---

### 4.4 크루 가입
```
POST /api/crew/signup
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "crewId": 1
}
```

**Response** `200 OK`
```json
{
  "message": "크루에 가입되었습니다"
}
```

---

### 4.5 내 크루 홈 조회
```
GET /api/crew
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "hasCrew": true,
  "crew": {
    "id": 1,
    "name": "아침 러너스",
    "intro": "아침에 달리는 크루입니다",
    "imgUrl": "https://example.com/image.jpg",
    "stats": {
      "totalDistance": 150000,
      "totalDuration": 54000,
      "memberCount": 15
    },
    "teamChallenge": {
      "id": 1,
      "title": "100km 달리기",
      "goalValue": 100000,
      "currentDistance": 45000,
      "progressPercentage": 45.0,
      "startAt": "2024-01-01T00:00:00",
      "endAt": "2024-01-31T23:59:59"
    },
    "todayMembers": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "imageUrl": "https://example.com/profile.jpg",
        "distance": 5000
      }
    ],
    "info": {
      "description": "크루 상세 설명",
      "rules": "크루 규칙"
    }
  }
}
```

**크루가 없는 경우** `200 OK`
```json
{
  "hasCrew": false,
  "crew": null
}
```

---

### 4.6 크루원 달린 기록 조회
```
GET /api/crew/{crewId}/members/stats
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `crewId`: 크루 ID

**Response** `200 OK`
```json
[
  {
    "userId": 1,
    "nickname": "홍길동",
    "imageUrl": "https://example.com/profile.jpg",
    "weekDistance": 15000,
    "monthDistance": 60000
  },
  {
    "userId": 2,
    "nickname": "김철수",
    "imageUrl": "https://example.com/profile2.jpg",
    "weekDistance": 12000,
    "monthDistance": 50000
  }
]
```

---

## 5. 러닝 기록 API

### 5.1 러닝 기록 생성
```
POST /api/v1/runnings
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "crewId": 1,
  "distance": 5000,
  "duration": 1800,
  "avgHeartrate": 145,
  "pace": 360
}
```

**Validation**
- `crewId`: 필수
- `distance`: 필수, 최소 1m
- `duration`: 필수, 최소 1초
- `avgHeartrate`: 선택
- `pace`: 선택 (초/km)

**Response** `201 Created`
```json
{
  "id": 1,
  "userId": 1,
  "crewId": 1,
  "distance": 5000,
  "duration": 1800,
  "avgHeartrate": 145,
  "pace": 360,
  "startedAt": "2024-01-15T09:00:00",
  "createdAt": "2024-01-15T09:30:00"
}
```

**후처리**
- 크루 챌린지 진행률 자동 갱신
- 배틀 리그 점수 자동 갱신

---

## 6. 챌린지 API

### 6.1 챌린지 생성
```
POST /api/v1/crews/{crewId}/challenges
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `crewId`: 크루 ID

**Request Body**
```json
{
  "title": "100km 달리기",
  "description": "이번 달 100km 완주 챌린지",
  "goalValue": 100000,
  "endDate": "2024-01-31T23:59:59"
}
```

**Validation**
- `title`: 필수
- `goalValue`: 필수, 최소 1000
- `endDate`: 필수, 미래 날짜
- `description`: 선택

**Response** `201 Created`
```json
{
  "challengeId": 1,
  "title": "100km 달리기",
  "description": "이번 달 100km 완주 챌린지",
  "goalValue": 100000,
  "currentAccumulatedDistance": 0,
  "status": "ACTIVE",
  "progressPercentage": 0.0,
  "startAt": "2024-01-01T10:00:00",
  "endAt": "2024-01-31T23:59:59"
}
```

---

### 6.2 챌린지 목록 조회
```
GET /api/v1/crews/{crewId}/challenges
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Path Parameters**
- `crewId`: 크루 ID

**Response** `200 OK`
```json
[
  {
    "challengeId": 1,
    "title": "100km 달리기",
    "description": "이번 달 100km 완주 챌린지",
    "goalValue": 100000,
    "currentAccumulatedDistance": 45000,
    "status": "ACTIVE",
    "progressPercentage": 45.0,
    "startAt": "2024-01-01T00:00:00",
    "endAt": "2024-01-31T23:59:59"
  },
  {
    "challengeId": 2,
    "title": "50km 달리기",
    "description": "지난 달 챌린지",
    "goalValue": 50000,
    "currentAccumulatedDistance": 52000,
    "status": "SUCCESS",
    "progressPercentage": 100.0,
    "startAt": "2023-12-01T00:00:00",
    "endAt": "2023-12-31T23:59:59"
  }
]
```

**챌린지 상태**
- `ACTIVE`: 진행 중
- `SUCCESS`: 성공 (목표 달성)
- `FAILED`: 실패 (기간 종료, 목표 미달성)

---

## 7. 배틀 리그 API

### 7.1 진행 중인 배틀 리그 조회
```
GET /api/v1/matches/ongoing
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "match": {
    "matchId": 1,
    "title": "1월 크루 대결",
    "description": "이번 달 최고의 크루는?",
    "startAt": "2024-01-01T00:00:00",
    "endAt": "2024-01-31T23:59:59"
  },
  "leaderboard": [
    {
      "rank": 1,
      "crewId": 1,
      "crewName": "아침 러너스",
      "totalDistance": 250000
    },
    {
      "rank": 2,
      "crewId": 2,
      "crewName": "저녁 산책",
      "totalDistance": 180000
    }
  ]
}
```

**진행 중인 매치가 없는 경우** `204 No Content`

---

### 7.2 진행 중인 배틀 리그 상세 조회
```
GET /api/v1/matches/ongoing/detail
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "match": {
    "matchId": 1,
    "title": "1월 크루 대결",
    "description": "이번 달 최고의 크루는?",
    "startAt": "2024-01-01T00:00:00",
    "endAt": "2024-01-31T23:59:59"
  },
  "myCrewDetail": {
    "crewId": 1,
    "crewName": "아침 러너스",
    "totalDistance": 250000,
    "memberContributions": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "distance": 50000,
        "rank": 1
      },
      {
        "userId": 2,
        "nickname": "김철수",
        "distance": 45000,
        "rank": 2
      }
    ]
  },
  "opponentCrewDetail": {
    "crewId": 2,
    "crewName": "저녁 산책",
    "totalDistance": 180000,
    "memberContributions": []
  }
}
```

**진행 중인 매치가 없거나 내 크루가 참가하지 않은 경우** `204 No Content`

**참고**
- `myCrewDetail`: 내 크루의 상세 정보 (크루원별 기여도 포함)
- `opponentCrewDetail`: 상대 크루의 요약 정보 (총 거리만 포함, 크루원 정보 제외)

---

## 8. 마이페이지 API

### 8.1 마이페이지 요약 조회
```
GET /api/mypage
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "nickname": "홍길동",
  "imageUrl": "https://example.com/profile.jpg",
  "joinDate": "2024.01.15",
  "totalDistanceKm": 245.8,
  "runningCount": 42,
  "averagePace": "7'30\""
}
```

---

## 공통 사항

### 인증
대부분의 API는 JWT 토큰을 사용한 인증이 필요합니다.

**Headers**
```
Authorization: Bearer {accessToken}
```

### 에러 응답

**400 Bad Request** - 잘못된 요청
```json
{
  "message": "유효성 검증 실패",
  "errors": [
    {
      "field": "email",
      "message": "이메일 형식이 올바르지 않습니다"
    }
  ]
}
```

**401 Unauthorized** - 인증 실패
```json
{
  "message": "인증이 필요합니다"
}
```

**404 Not Found** - 리소스를 찾을 수 없음
```json
{
  "message": "크루를 찾을 수 없습니다"
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "message": "서버 오류가 발생했습니다"
}
```

---

## 데이터 단위

- **거리**: 미터(m) 단위로 저장/전송
- **시간**: 초(s) 단위로 저장/전송
- **페이스**: 초/km 단위 (예: 360 = 6분/km)
- **날짜/시간**: ISO 8601 형식 (`yyyy-MM-ddTHH:mm:ss`)

---

## 버전 정보

- **API 버전**: v1
- **최종 업데이트**: 2024-01-15
- **Base URL**: `http://localhost:8080`

---

## 변경 이력

### 2024-01-16
- 홈 화면 API 추가 (배틀 리그 간략 정보, 오늘의 러닝 기록, 최근 크루 활동)
- 배틀 리그 상세 조회 시 상대 크루원 정보 비공개 처리

### 2024-01-15
- 챌린지 API 추가
- 배틀 리그 API 추가
- 러닝 기록 자동 진행률 갱신 기능 추가
- 마이페이지 API 추가

### 초기 버전
- 인증 API
- 사용자 API
- 크루 API
- 러닝 기록 API
