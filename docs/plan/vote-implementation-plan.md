# 투표 기능 구현 계획
> 헥사고날 아키텍처 + DDD 기반. 채팅 도메인이 mock으로 만든 Vote 관련 코드를 흡수하여 정식 구현. 서비스 함수는 inbound port (UseCase)로만 노출.
> 알림 / PUSH 기능은 본 명세에서 제외 (PWA 환경 정책 확정 후 별도 명세).

---

## 1. 패키지 구조

```
com.ject.vs
├── common
│   └── domain
│       ├── BaseEntity.java          # 채팅 명세에서 정의됨 — 재사용
│       ├── BaseTimeEntity.java      # 채팅 명세에서 정의됨 — 재사용
│       └── TimeTrackable.java       # 채팅 명세에서 정의됨 — 재사용
│
├── vote                             # Vote Bounded Context (본 문서 범위)
│   ├── domain
│   │   ├── Vote.java                            # 채팅 mock 흡수 + 확장
│   │   ├── VoteOption.java
│   │   ├── VoteParticipation.java               # 채팅 mock 흡수 + 확장
│   │   ├── VoteEmojiReaction.java
│   │   ├── GuestFreeVote.java
│   │   ├── VoteStatus.java                      # enum
│   │   ├── VoteType.java                        # enum
│   │   ├── VoteDuration.java                    # enum (HOURS_12, HOURS_24)
│   │   ├── VoteEmoji.java                       # enum
│   │   ├── VoteRepository.java                  # Spring Data JPA
│   │   ├── VoteOptionRepository.java
│   │   ├── VoteParticipationRepository.java     # Spring Data JPA
│   │   ├── VoteEmojiReactionRepository.java
│   │   └── GuestFreeVoteRepository.java
│   ├── port
│   │   ├── in
│   │   │   ├── VoteQueryUseCase.java            # 채팅이 의존할 inbound port
│   │   │   ├── VoteCommandUseCase.java          # 업로드 / 참여 / 취소
│   │   │   ├── ImmersiveVoteQueryUseCase.java
│   │   │   ├── ImmersiveVoteCommandUseCase.java
│   │   │   ├── VoteResultQueryUseCase.java
│   │   │   └── VoteEmojiCommandUseCase.java
│   │   ├── VoteQueryService.java
│   │   ├── VoteCommandService.java
│   │   ├── ImmersiveVoteQueryService.java
│   │   ├── ImmersiveVoteCommandService.java
│   │   ├── VoteResultQueryService.java
│   │   └── VoteEmojiCommandService.java
│   ├── adapter
│   │   └── web
│   │       ├── VoteController.java
│   │       ├── ImmersiveVoteController.java
│   │       ├── VoteResultController.java
│   │       ├── VoteEmojiController.java
│   │       └── GuestFreeVoteController.java
│   └── scheduler
│       └── VoteCloseScheduler.java              # ENDED 상태 전환만 (알림 발송 X)
│
└── config
    ├── WebSocketConfig.java                     # 채팅 명세에서 정의됨 — 재사용
    ├── WebSocketAuthInterceptor.java            # 채팅 명세에서 정의됨 — 재사용
    └── AnonymousIdResolver.java                 # 비회원 쿠키 식별 (신규)
```

---

## 2. 공통 기반

채팅 명세에서 정의한 `BaseEntity` / `BaseTimeEntity` / `TimeTrackable`을 그대로 재사용.

```
BaseEntity (id)
    └── BaseTimeEntity (id + createdAt + updatedAt)  implements TimeTrackable
```

각 엔티티별 상속 결정:

| 엔티티 | 상속 | 이유 |
|--------|------|------|
| `Vote` | `BaseTimeEntity` | createdAt 응답 노출 + 종료 처리에 endAt 비교 필요 |
| `VoteOption` | `BaseEntity` | 시간 추적 불필요. position만 관리 |
| `VoteParticipation` | `BaseTimeEntity` | createdAt = 투표 시각. 분석 쿼리에 필요 |
| `VoteEmojiReaction` | `BaseTimeEntity` | updatedAt = 마지막 이모지 교체 시각 |
| `GuestFreeVote` | `BaseTimeEntity` | last_consumed_at 추적 |

---

## 3. 단계별 구현

### STEP 1 — Vote Bounded Context 흡수 + 확장

**목표:** 채팅 도메인이 mock으로 만든 Vote 관련 코드를 정식 구현으로 대체. inbound port (`VoteQueryUseCase`) 노출.

**구현 파일**

| 파일 | 내용 |
|------|------|
| `vote/domain/Vote.java` | extends BaseTimeEntity. type, title, content, thumbnailUrl, imageUrl, duration, status, endAt |
| `vote/domain/VoteOption.java` | extends BaseEntity. voteId FK + label + position |
| `vote/domain/VoteParticipation.java` | extends BaseTimeEntity. (voteId, userId / anonymousId, optionId) |
| `vote/domain/VoteStatus.java` | enum (ONGOING, ENDED) |
| `vote/domain/VoteType.java` | enum (GENERAL, IMMERSIVE) |
| `vote/domain/VoteDuration.java` | enum (HOURS_12=12, HOURS_24=24) |
| `vote/domain/VoteRepository.java` | Spring Data JPA |
| `vote/domain/VoteOptionRepository.java` | Spring Data JPA |
| `vote/domain/VoteParticipationRepository.java` | 채팅 mock 인터페이스 흡수 + 확장 |
| `vote/port/in/VoteQueryUseCase.java` | inbound port — Chat이 의존 |
| `vote/port/VoteQueryService.java` | implements VoteQueryUseCase |
| `db/migration/V3__vote_schema.sql` | vote, vote_option, vote_participation 테이블 + 채팅 mock 마이그레이션 |

**Vote 도메인 핵심 함수**

```java
@Entity
public class Vote extends BaseTimeEntity {
    @Enumerated(EnumType.STRING)
    private VoteType type;
    private String title;
    private String content;
    private String thumbnailUrl;
    private String imageUrl;
    @Enumerated(EnumType.STRING)
    private VoteDuration duration;
    @Enumerated(EnumType.STRING)
    private VoteStatus status;
    private LocalDateTime endAt;

    // 업로드 팩토리 — endAt 자체 계산
    public static Vote create(VoteType type, String title, String content,
                              String thumbnailUrl, String imageUrl,
                              VoteDuration duration) {
        if (type == VoteType.IMMERSIVE && imageUrl == null) {
            throw new ImageRequiredException();
        }
        Vote vote = new Vote();
        vote.type = type;
        vote.title = title;
        vote.content = content;
        vote.thumbnailUrl = thumbnailUrl;
        vote.imageUrl = imageUrl;
        vote.duration = duration;
        vote.status = VoteStatus.ONGOING;
        vote.endAt = LocalDateTime.now().plusHours(duration.getHours());
        return vote;
    }

    public boolean isEnded() {
        return status == VoteStatus.ENDED || endAt.isBefore(LocalDateTime.now());
    }

    public boolean isOngoing() {
        return !isEnded();
    }

    public void close() {
        this.status = VoteStatus.ENDED;
    }
}
```

**VoteDuration enum**

```java
@Getter
@RequiredArgsConstructor
public enum VoteDuration {
    HOURS_12(12),
    HOURS_24(24);

    private final int hours;

    public static VoteDuration from(int hours) {
        return Arrays.stream(values())
            .filter(d -> d.hours == hours)
            .findFirst()
            .orElseThrow(InvalidDurationException::new);
    }
}
```

`durationHours` 12/24 검증을 enum의 `from(int)` 팩토리에 위임. Service / Controller에서 if-else 체크 안 함.

**VoteParticipation 도메인 핵심 함수**

```java
@Entity
public class VoteParticipation extends BaseTimeEntity {
    private Long voteId;
    private Long userId;          // 회원이면 set
    private String anonymousId;   // 비회원이면 set
    private Long optionId;

    public static VoteParticipation ofMember(Long voteId, Long userId, Long optionId) { ... }
    public static VoteParticipation ofGuest(Long voteId, String anonymousId, Long optionId) { ... }

    public boolean isGuest() { return anonymousId != null; }

    public void changeOption(Long optionId) { this.optionId = optionId; }
}
```

**VoteQueryUseCase (inbound port — Chat 도메인이 의존)**

```java
public interface VoteQueryUseCase {
    boolean isParticipated(Long voteId, Long userId);
    Optional<Long> getSelectedOptionId(Long voteId, Long userId);
    VoteSummary getVoteSummary(Long voteId);
    VoteRatio getRatio(Long voteId);
    int getParticipantCount(Long voteId);
}
```

**테스트**
- `VoteTest`: `create()` 팩토리 (endAt 계산, IMMERSIVE면 imageUrl 필수), `isEnded()`, `close()` 검증
- `VoteDurationTest`: `from(12)`, `from(24)`, `from(13)` 시 예외 검증
- `VoteParticipationTest`: `ofMember()`, `ofGuest()` 팩토리 + `changeOption()` 검증
- `VoteQueryServiceTest`: 참여 여부 / 선택 옵션 조회 / 비율 계산 검증
- `VoteParticipationRepositoryTest`: `@DataJpaTest` — 회원/비회원 식별 컬럼 분기, unique constraint 검증

---

### STEP 2 — 비회원 쿠키 식별 인프라

**목표:** 모든 API 진입점에서 회원 / 비회원 식별 가능하도록 인프라 구축.

**구현 파일**

| 파일 | 내용 |
|------|------|
| `config/AnonymousIdResolver.java` | `HandlerMethodArgumentResolver` — 쿠키에서 anonymous_id 추출, 없으면 신규 발급 + Set-Cookie |
| `vote/domain/GuestFreeVote.java` | extends BaseTimeEntity. anonymousId PK + consumedCount |
| `vote/domain/GuestFreeVoteRepository.java` | Spring Data JPA |

**AnonymousIdResolver 동작 흐름**

```java
@Component
public class AnonymousIdResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AnonymousId.class);
    }

    @Override
    public String resolveArgument(...) {
        HttpServletRequest req = ...;
        HttpServletResponse res = ...;

        // 1. 쿠키에서 anonymous_id 추출
        String existingId = extractFromCookie(req, "anonymous_id");
        if (existingId != null) return existingId;

        // 2. 없으면 신규 발급 + Set-Cookie
        String newId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from("anonymous_id", newId)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .maxAge(Duration.ofDays(365))
            .path("/")
            .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return newId;
    }
}
```

**Controller 사용 예시**

```java
@PostMapping("/api/votes/{voteId}/participate")
public ParticipateResponse participate(
        @PathVariable Long voteId,
        @AuthenticationPrincipal Long userId,            // 회원이면 not null
        @AnonymousId String anonymousId,                  // 비회원이면 사용
        @RequestBody ParticipateRequest request) {
    if (userId != null) {
        return voteCommandUseCase.participateAsMember(voteId, userId, request.optionId());
    }
    return voteCommandUseCase.participateAsGuest(voteId, anonymousId, request.optionId());
}
```

**테스트**
- `AnonymousIdResolverTest`: 쿠키 존재 / 부재 케이스, Set-Cookie 헤더 발급 검증
- `GuestFreeVoteRepositoryTest`: upsert 동시성, 5회 소진 시 차감 차단 검증

---

### STEP 3 — 일반형 투표 Application Services

**목표:** 일반형 투표의 업로드 / 조회 / 참여 / 취소 / 이모지 비즈니스 로직.

**VoteCommandUseCase (inbound port)**

```java
public interface VoteCommandUseCase {
    VoteCreateResult create(VoteCreateCommand command);
    ParticipateResult participateAsMember(Long voteId, Long userId, Long optionId);
    ParticipateResult participateAsGuest(Long voteId, String anonymousId, Long optionId);
    void cancel(Long voteId, Long userId);
}

public record VoteCreateCommand(
    VoteType type,
    String title,
    String content,
    String thumbnailUrl,
    String imageUrl,
    int durationHours,
    String optionA,
    String optionB
) { }
```

**VoteCommandService 핵심 로직 — 업로드**

```java
@Service
@Transactional
@RequiredArgsConstructor
public class VoteCommandService implements VoteCommandUseCase {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;

    @Override
    public VoteCreateResult create(VoteCreateCommand cmd) {
        // 1. duration 검증 (enum 팩토리에 위임)
        VoteDuration duration = VoteDuration.from(cmd.durationHours());

        // 2. Vote 생성 (도메인 팩토리에서 endAt 계산 + IMMERSIVE면 imageUrl 검증)
        Vote vote = Vote.create(
            cmd.type(), cmd.title(), cmd.content(),
            cmd.thumbnailUrl(), cmd.imageUrl(), duration
        );
        Vote saved = voteRepository.save(vote);

        // 3. 옵션 A / B 생성
        voteOptionRepository.save(VoteOption.of(saved.getId(), cmd.optionA(), 0));
        voteOptionRepository.save(VoteOption.of(saved.getId(), cmd.optionB(), 1));

        return VoteCreateResult.from(saved);
    }
}
```

**VoteCommandService 핵심 로직 — 비회원 참여**

```java
@Override
public ParticipateResult participateAsGuest(Long voteId, String anonymousId, Long optionId) {
    Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
    if (vote.isEnded()) throw new VoteEndedException();

    if (!voteOptionRepository.existsByIdAndVoteId(optionId, voteId))
        throw new InvalidOptionException();

    Optional<VoteParticipation> existing =
        voteParticipationRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);

    if (existing.isPresent()) {
        // 옵션 변경 — 차감하지 않음
        existing.get().changeOption(optionId);
        return ParticipateResult.from(existing.get(), getRemaining(anonymousId));
    }

    // 신규 — GuestFreeVote 차감
    guestFreeVoteService.consume(anonymousId);  // 5회 초과 시 VoteFreeLimitExceededException

    VoteParticipation saved = voteParticipationRepository.save(
        VoteParticipation.ofGuest(voteId, anonymousId, optionId));
    return ParticipateResult.from(saved, getRemaining(anonymousId));
}
```

**구현 파일**

| 파일 | 내용 |
|------|------|
| `vote/port/in/VoteCommandUseCase.java` | inbound port (create + participate + cancel) |
| `vote/port/VoteCommandService.java` | 비즈니스 로직 |
| `vote/port/in/VoteEmojiCommandUseCase.java` | 이모지 반응 |
| `vote/port/VoteEmojiCommandService.java` | 이모지 upsert / 취소 |

**테스트**
- `VoteCommandServiceTest`:
  - `create`: 정상 / IMMERSIVE인데 imageUrl 누락 / durationHours 13 → 예외
  - `participate`: 신규 / 옵션 변경 / 5회 소진 시 차단 / ENDED 차단 / 옵션 무효
- `VoteEmojiCommandServiceTest`: 다른 이모지 교체 / 동일 이모지 재선택 시 취소 / null 전송 시 취소

---

### STEP 4 — 몰입형 투표 Application Services

**목표:** 몰입형 피드 + 단일 엔드포인트 참여 / 취소.

> 몰입형 투표 업로드도 `POST /api/votes` 엔드포인트 하나로 처리. `type: IMMERSIVE`로 분기.

**ImmersiveVoteCommandUseCase (inbound port)**

```java
public interface ImmersiveVoteCommandUseCase {
    ImmersiveParticipateResult participateOrCancel(Long voteId, Long userId, String anonymousId, Long optionId);
}

public record ImmersiveParticipateResult(
    ImmersiveVoteAction action,   // VOTED | CANCELED
    Long selectedOptionId,
    List<OptionRatio> options,
    Integer remainingFreeVotes
) { }
```

**핵심 로직**

```java
@Override
public ImmersiveParticipateResult participateOrCancel(Long voteId, Long userId, String anonymousId, Long optionId) {
    Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
    if (vote.isEnded()) throw new VoteEndedException();

    Optional<VoteParticipation> existing = userId != null
        ? voteParticipationRepository.findByVoteIdAndUserId(voteId, userId)
        : voteParticipationRepository.findByVoteIdAndAnonymousId(voteId, anonymousId);

    if (existing.isPresent() && existing.get().getOptionId().equals(optionId)) {
        // 같은 옵션 재클릭 → 취소
        voteParticipationRepository.delete(existing.get());
        return ImmersiveParticipateResult.canceled(...);
    }

    if (existing.isPresent()) {
        // 옵션 변경 → 차감 X
        existing.get().changeOption(optionId);
        return ImmersiveParticipateResult.voted(...);
    }

    // 신규 → 비회원이면 차감
    if (userId == null) guestFreeVoteService.consume(anonymousId);
    voteParticipationRepository.save(...);
    return ImmersiveParticipateResult.voted(...);
}
```

**구현 파일**

| 파일 | 내용 |
|------|------|
| `vote/port/in/ImmersiveVoteQueryUseCase.java` | 피드 조회 (cursor 페이지네이션) |
| `vote/port/in/ImmersiveVoteCommandUseCase.java` | 참여 / 취소 단일 메서드 |
| `vote/port/ImmersiveVoteQueryService.java` | 피드 + currentViewerCount 집계 |
| `vote/port/ImmersiveVoteCommandService.java` | 위 로직 구현 |

**테스트**
- `ImmersiveVoteCommandServiceTest`: VOTED / CANCELED 분기, 회원 / 비회원별 차감 정책, ENDED 차단

---

### STEP 5 — 결과 화면 (Insight)

**목표:** 마감된 투표의 결과 + 인사이트 분석 + AI Insight.

**VoteResultQueryUseCase (inbound port)**

```java
public interface VoteResultQueryUseCase {
    VoteResultDetail getResult(Long voteId, Long userId, String anonymousId);
}
```

**Insight 분기 로직**

```java
@Override
public VoteResultDetail getResult(Long voteId, Long userId, String anonymousId) {
    Vote vote = voteRepository.findById(voteId).orElseThrow(VoteNotFoundException::new);
    if (vote.isOngoing()) throw new VoteNotEndedException();

    // 결과 옵션 + 비율
    VoteResult result = computeResult(voteId);

    // 인사이트 분기
    Insight insight;
    if (userId == null) {
        // 비회원 — 잠금
        insight = Insight.locked();
    } else {
        Optional<VoteParticipation> myVote =
            voteParticipationRepository.findByVoteIdAndUserId(voteId, userId);
        insight = myVote.isPresent()
            ? buildMySelectionInsight(voteId, myVote.get(), userId)  // scope=MY_SELECTION
            : buildTotalInsight(voteId);                              // scope=TOTAL
    }

    AiInsight aiInsight = (userId != null && hasParticipated(voteId, userId))
        ? aiInsightService.generateOrFetch(voteId, userId)
        : AiInsight.unavailable();

    return VoteResultDetail.of(vote, result, myVote, insight, aiInsight);
}
```

**Insight 분석 쿼리 — `genderDistribution`, `ageDistribution`**

User 테이블의 gender / birthDate 컬럼이 필요. (분석 데이터 수집 항목 추가 필요 — 회의에서 이미 리스크로 식별됨.)

```sql
-- MY_SELECTION 케이스 — 본인이 선택한 옵션 기준 성별 분포
SELECT u.gender, COUNT(*) AS count
FROM vote_participation vp
JOIN users u ON vp.user_id = u.id
WHERE vp.vote_id = :voteId
  AND vp.option_id = :myOptionId
GROUP BY u.gender;
```

**추가 예정 : AI Insight 캐싱 전략**
- 첫 호출 시 LLM 호출 → vote 테이블의 `ai_insight_headline`, `ai_insight_body` 컬럼에 저장
- 이후 호출은 컬럼에서 직접 반환
- LLM 호출 실패 시 `aiInsight.available: false`로 응답 (에러 아님)
- 본인이 만든 youth-policy AI 에이전트 (LangChain4j + Gemini 2.5 Flash) 인프라 재활용 검토

**구현 파일**

| 파일 | 내용 |
|------|------|
| `vote/port/in/VoteResultQueryUseCase.java` | 결과 조회 inbound port |
| `vote/port/VoteResultQueryService.java` | Insight 분기 + AI Insight 캐싱 |
| `vote/adapter/web/VoteResultController.java` | `/result`, `/share` |

---

### STEP 6 — 투표 종료 스케줄러

**목표:** 마감 시각 도래한 투표를 ENDED 상태로 전환.

> 본 명세에서는 알림 발송 없이 **상태 전환만** 처리. 알림 발송은 PWA 푸시 명세 확정 후 별도 단계로 추가.

**구현 파일**

| 파일 | 내용 |
|------|------|
| `vote/scheduler/VoteCloseScheduler.java` | `@Scheduled` 로 ENDED 전환 |

**스케줄러 흐름**

```java
@Component
@RequiredArgsConstructor
public class VoteCloseScheduler {

    private final VoteRepository voteRepository;

    @Scheduled(cron = "0 * * * * *")  // 매분
    @Transactional
    public void closeExpiredVotes() {
        List<Vote> expired = voteRepository.findExpiredOngoing(LocalDateTime.now());
        for (Vote vote : expired) {
            vote.close();
        }
    }

    // 서버 재시작 직후 누락 보정용 1회 실행
    @PostConstruct
    public void closeExpiredOnStartup() {
        closeExpiredVotes();
    }
}
```

> 회의에서 이미 식별된 리스크: 서버 재시작 중 스케줄러 누락. `@PostConstruct`로 시작 시 1회 실행하여 미전환 vote 일괄 처리.

**테스트**
- `VoteCloseSchedulerTest`: 마감 시각 도래한 vote만 close 검증
- 시작 시 미전환 vote 보정 검증

---

### STEP 7 — REST Controllers

**목표:** REST 엔드포인트 노출. UseCase 주입 + DTO 변환.

**Controller 구조 — VoteController 예시**

```java
@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteQueryUseCase voteQueryUseCase;
    private final VoteCommandUseCase voteCommandUseCase;

    @PostMapping
    public VoteCreateResponse create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteCreateRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return VoteCreateResponse.from(
            voteCommandUseCase.create(request.toCommand()));
    }

    @GetMapping("/{voteId}")
    public VoteDetailResponse getDetail(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId) { ... }

    @PostMapping("/{voteId}/participate")
    public ParticipateResponse participate(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId,
            @AnonymousId String anonymousId,
            @RequestBody ParticipateRequest request) { ... }

    @DeleteMapping("/{voteId}/participate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long voteId,
            @AuthenticationPrincipal Long userId) { ... }
}
```

**Controller 분리**

| 파일 | 엔드포인트 |
|------|------------|
| `VoteController` | `POST /api/votes`, `/api/votes/{voteId}/...` (일반형) |
| `ImmersiveVoteController` | `/api/immersive-votes/...` |
| `VoteResultController` | `/api/votes/{voteId}/result`, `/share` |
| `VoteEmojiController` | `PUT /api/votes/{voteId}/emoji`, `PUT /api/immersive-votes/{voteId}/emoji` |
| `GuestFreeVoteController` | `GET /api/me/free-votes` |

**테스트**
- `*ControllerTest`: `@WebMvcTest` — 각 엔드포인트 요청/응답 검증, 401/403 케이스, anonymousId 쿠키 발급 검증

---

### STEP 8 — WebSocket 몰입형 실시간 비율

**목표:** `/topic/immersive-vote/{voteId}/live` broadcast.

채팅 명세에서 정의된 `WebSocketConfig` / `WebSocketAuthInterceptor` 그대로 재사용. 단 본 broadcast는 익명 수신 허용 (인증 불필요).

**broadcast 흐름**

```
POST /api/immersive-votes/{voteId}/participate
        │
        ▼
ImmersiveVoteCommandService.participateOrCancel()
        │
        ▼
[비율 변경 후]
SimpMessagingTemplate.convertAndSend("/topic/immersive-vote/" + voteId + "/live", payload)
```

**도입 시점**
- 1차: REST 폴링 (`GET /live`) 으로 시작
- 2차: 동시 접속자 증가 시 WebSocket 전환. 클라이언트 측 분기 필요

**테스트**
- `ImmersiveVoteWebSocketIntegrationTest`: REST POST 후 `/topic/immersive-vote/{voteId}/live` 수신 검증

---

## 4. Flyway 마이그레이션

| 파일 | 내용 |
|------|------|
| `V1__init_schema.sql` | 기존 (users, token) |
| `V2__chat_schema.sql` | 채팅 명세 — vote (mock), vote_participation (mock), chat_message, chat_room_unread |
| `V3__vote_schema.sql` | **본 명세** — vote 테이블 컬럼 추가 (type, title, content, thumbnail_url, image_url, duration, status, end_at) + vote_option, vote_emoji_reaction, guest_free_vote 신규 + vote_participation 컬럼 확장 (anonymous_id, option_id) + users.gender, users.birthDate 컬럼 추가 (Insight 분석용) |

> V2에서 mock으로 만든 vote / vote_participation 테이블은 V3에서 ALTER로 컬럼 추가. 데이터 백워드 호환성 유지.

---

## 5. 테스트 전략 요약

| 레이어 | 도구 | 대상 |
|--------|------|------|
| Domain | JUnit 5 (순수 단위) | 팩토리 메서드, 도메인 검증 함수 (`Vote.create`, `isEnded`, `VoteDuration.from` 등) |
| Application | JUnit 5 + Mockito | UseCase 비즈니스 로직, 차감 정책, 예외 케이스 |
| Persistence | `@DataJpaTest` | 회원 / 비회원 식별 분기, unique constraint, cursor 페이지네이션 |
| Controller | `@WebMvcTest` | 요청/응답 직렬화, 인증 / 비회원 쿠키, 401/403 케이스 |
| Scheduler | `@SpringBootTest` | 마감 시각 도래한 vote ENDED 전환 |
| WebSocket | `StompClient` 통합 | REST POST → `/topic/immersive-vote/{voteId}/live` 수신 end-to-end |

---

## 6. 구현 순서 요약

```
STEP 1  Vote BC 흡수 + 확장 (VoteQueryUseCase 노출 — Chat이 의존)
        + Vote 도메인 팩토리 (create) — endAt 자체 계산, IMMERSIVE 검증
        + VoteDuration enum (HOURS_12 / HOURS_24)

STEP 2  비회원 쿠키 식별 인프라 (AnonymousIdResolver + GuestFreeVote)

STEP 3  일반형 투표 Application Services (업로드 / 조회 / 참여 / 취소 / 이모지)

STEP 4  몰입형 투표 Application Services (피드 / 단일 엔드포인트 참여·취소)
        업로드는 STEP 3의 POST /api/votes 엔드포인트 재사용 (type: IMMERSIVE)

STEP 5  결과 화면 + Insight (MY_SELECTION / TOTAL / locked) + AI Insight 캐싱

STEP 6  투표 종료 스케줄러 (ENDED 상태 전환만. 알림 발송은 별도 명세)

STEP 7  REST Controllers (5개)

STEP 8  WebSocket 몰입형 실시간 비율 (폴링 → 실시간 전환)
```

---

## 7. 채팅 도메인 협업 사항

채팅 명세에서 mock으로 만든 Vote 관련 코드를 본 도메인이 흡수. `ChatCommandService`는 outbound port (`VoteParticipationRepository`) 직접 의존에서 → 본 도메인이 노출하는 inbound port (`VoteQueryUseCase`) 의존으로 변경.

**코드 변경 예시**

```java
// Before
@RequiredArgsConstructor
public class ChatCommandService {
    private final VoteParticipationRepository voteParticipationRepository;

    public void sendMessage(...) {
        if (!voteParticipationRepository.existsByVoteIdAndUserId(voteId, senderId))
            throw new ChatForbiddenException();
    }
}

// After
@RequiredArgsConstructor
public class ChatCommandService {
    private final VoteQueryUseCase voteQueryUseCase;  // ← inbound port 의존

    public void sendMessage(...) {
        if (!voteQueryUseCase.isParticipated(voteId, senderId))
            throw new ChatForbiddenException();
    }
}
```

흡수 / 삭제 대상 (mock으로 만든 것):
- `vote/domain/Vote.java` → 본 명세 Vote로 대체 (확장)
- `vote/domain/VoteParticipation.java` → 본 명세 VoteParticipation으로 대체 (확장)
- `vote/domain/VoteRepository.java` → 본 명세 Repository로 대체
- `vote/domain/VoteParticipationRepository.java` → 본 명세 Repository로 대체

> 별도 PR로 진행 권장. STEP 1 완료 시점에 import 경로 변경 PR 요청.

---

## 8. 별도 명세로 분리된 항목

다음 기능은 본 구현 계획에 포함되지 않음:

- **알림 / PUSH** — PWA Web Push API + VAPID 기반 재설계 필요. 기획자 / 프론트와 정책 합의 후 별도 명세로 분리
  - 합의 대상: iOS 16.4 미만 사용자 처리, "홈 화면 추가" 유도 UI, 권한 요청 타이밍, 라이브러리 (`nl.martijndwars:web-push` 검토)
  - 백엔드 구현 범위: VAPID 키 발급 / 관리, `web_push_subscription` 테이블, 발송 시 만료 subscription 자동 정리
  - Vote 도메인 연결 지점: 투표 종료 스케줄러 (`VoteCloseScheduler`)에서 알림 도메인 inbound port 호출로 트리거
- **이미지 업로드** (`POST /api/uploads/image`) — Pre-signed URL 방식 / 직접 업로드 방식 결정 후 명세
