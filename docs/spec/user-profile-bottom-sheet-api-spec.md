# 유저 프로필 바텀시트 API 명세 — 다른 유저 프로필 탐색

> 기반: 채팅 화면 > 닉네임 탭 > 유저 프로필 바텀시트 (페이지명: 유저 프로필 바텀시트)
> 버전: 1.0
> 대상: 회원 (비회원 진입 불가)

## 1. 요구사항 핵심 요약

### 1-1. 프로필 바텀시트 진입
- 채팅 메시지의 **프로필 이미지** 또는 **닉네임** 탭 시 노출
- **비회원**은 진입 불가 (FE에서 차단, API도 인증 필수)
- 진입 가능 화면: 전체 채팅 / 몰입형 투표 바텀시트형 채팅 / 일반형 투표 바텀시트형 채팅 (동일 바텀시트)

### 1-2. 참여투표 영역
- 상단: 참여투표 **총 개수** 표시 (예: `참여투표 24`)
- 카드 최대 **3개** 노출 (더보기 없음)
- 정렬: **최신 활동순** (투표 생성 시각 ❌ → 해당 유저의 참여/활동 시각 ⭕)
- 카드 구성:
  - 투표 상태 라벨: `ONGOING`(진행중) / `ENDED`(종료)
  - 투표 제목 (1줄 말줄임 — FE 처리)
  - **프로필 대상 유저**가 선택한 선택지 텍스트 (체크 아이콘 + 1줄 말줄임 — FE 처리)
  - 우측 화살표 `>` (FE)
- 1~2개만 있으면 있는 만큼만 노출 (빈 슬롯 없음)

### 1-3. 투표 리스트 카드 탭 — 화면 랜딩 (FE)
| 투표 상태 | 투표 타입 | 조회자(본인) 참여 | 랜딩 화면 |
|-----------|-----------|-------------------|-----------|
| 진행중 | 일반형 | - | 일반형 투표_회원 |
| 진행중 | 몰입형 | - | 몰입형 투표_회원 |
| 종료 | - | 참여 O | 투표결과_회원_참여O |
| 종료 | - | 참여 X | 투표결과_회원_참여X |

- 삭제된 투표 탭 시: `삭제된 투표입니다` Error 토스트 (기존 `GET /api/votes/{voteId}` 404 활용)

### 1-4. FE 전달사항 (서버 비관여)
- 바텀시트 고정 높이 500px
- Dim Layer: `#000000` 40%
- 다크 모드 외곽선 Stroke
- 투표 리스트 전체 Frame 터치 영역

---

## 2. 최신 활동순 정렬 기준 (서버)

프로필 **대상 유저**(`targetUserId`) 기준, 투표별 마지막 활동 시각:

```
lastActivityAt = GREATEST(
  vote_participation.updated_at,
  COALESCE(
    (SELECT MAX(chat_message.created_at)
     FROM chat_message
     WHERE sender_id = targetUserId AND vote_id = participation.vote_id),
    vote_participation.created_at
  )
)
```

- 참여만 하고 채팅이 없으면 `participation.created_at` 사용
- 선택지 변경 시 `participation.updated_at` 반영
- 채팅 메시지 전송 시 해당 시각이 더 최신이면 채팅 시각 우선

---

## 3. 투표 타입 판별

| 조건 | voteType |
|------|----------|
| `vote.image_url IS NOT NULL` | `IMMERSIVE` |
| `vote.image_url IS NULL` | `GENERAL` |

> 기존 도메인 관례와 동일 (`ImmersiveVoteQueryService` 폴백 로직 참고)

---

## 4. REST API

### GET `/api/users/{userId}/profile-sheet` — 다른 유저 프로필 바텀시트 조회

**인증**: 필수 (회원 JWT). 비회원 401.

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `userId` | `Long` | 프로필 조회 대상 유저 ID |

**Response 200**

```json
{
  "userId": 42,
  "nickname": "승부사",
  "imageColor": "GREEN",
  "participatedVoteCount": 24,
  "recentParticipatedVotes": [
    {
      "voteId": 101,
      "title": "직장인 점심시간 혼밥 vs 같이 먹기",
      "status": "ONGOING",
      "voteType": "GENERAL",
      "selectedOptionLabel": "혼밥이 편하다",
      "viewerParticipated": true
    },
    {
      "voteId": 88,
      "title": "주말엔 집콕 vs 나들이",
      "status": "ENDED",
      "voteType": "IMMERSIVE",
      "selectedOptionLabel": "나들이가 좋다",
      "viewerParticipated": false
    }
  ]
}
```

**필드 설명**

| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | `Long` | 대상 유저 ID |
| `nickname` | `String` | 닉네임. 탈퇴 유저는 `알 수 없음` |
| `imageColor` | `ImageColor \| null` | 프로필 아이콘 색상. 탈퇴 유저는 `null` |
| `participatedVoteCount` | `long` | 참여투표 총 개수 |
| `recentParticipatedVotes` | `Array` | 최신 활동순 상위 3개 (0~3개) |
| `recentParticipatedVotes[].voteId` | `Long` | 투표 ID (카드 탭 시 랜딩용) |
| `recentParticipatedVotes[].title` | `String` | 투표 제목 |
| `recentParticipatedVotes[].status` | `ONGOING \| ENDED` | 투표 상태 |
| `recentParticipatedVotes[].voteType` | `GENERAL \| IMMERSIVE` | 투표 타입 (랜딩 분기용) |
| `recentParticipatedVotes[].selectedOptionLabel` | `String` | **대상 유저** 선택지 텍스트 |
| `recentParticipatedVotes[].viewerParticipated` | `boolean` | **조회자(본인)** 참여 여부 (종료 투표 랜딩 분기용) |

**에러**

| HTTP | code | 상황 |
|------|------|------|
| 401 | `UNAUTHORIZED` | 비회원 |
| 404 | `E400000` | 존재하지 않는 `userId` |

---

## 5. 구현 구조

```
user/
├── adapter/web/
│   ├── UserController.java          # GET /api/users/{userId}/profile-sheet
│   └── dto/
│       └── UserProfileBottomSheetResponse.java
├── port/
│   ├── UserProfileQueryService.java
│   └── in/
│       └── UserProfileQueryUseCase.java
vote/
├── domain/
│   ├── VoteType.java                # GENERAL | IMMERSIVE
│   └── VoteParticipationRepository.java  # findTop3VoteIdsByRecentActivity
```

**서비스 흐름**
1. `targetUserId` 유저 존재 확인 (`UserQueryUseCase.getUser`)
2. `countByUserId(targetUserId)` → `participatedVoteCount`
3. `findTop3VoteIdsByRecentActivity(targetUserId)` → voteId 목록
4. Vote / VoteOption / Participation 일괄 조회 후 DTO 조립
5. 각 vote에 대해 `existsByVoteIdAndUserId(voteId, viewerUserId)` → `viewerParticipated`

---

## 6. FE 연동 가이드

### 바텀시트 오픈
```
GET /api/users/{senderId}/profile-sheet
Authorization: Bearer {accessToken}
```
- 채팅 메시지의 `senderId`를 path `userId`로 전달

### 카드 탭 랜딩 분기 (클라이언트)
```typescript
function resolveLanding(vote: RecentParticipatedVote, currentScreenType: VoteScreenType) {
  if (vote.status === 'ONGOING') {
    return vote.voteType === 'IMMERSIVE' ? 'IMMERSIVE_MEMBER' : 'GENERAL_MEMBER';
  }
  return vote.viewerParticipated ? 'RESULT_MEMBER_JOINED' : 'RESULT_MEMBER_NOT_JOINED';
}
```

### 삭제된 투표
- 카드 탭 후 `GET /api/votes/{voteId}` 또는 결과 API 호출 시 404 → 토스트 `삭제된 투표입니다`