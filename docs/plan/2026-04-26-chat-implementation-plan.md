# 채팅 기능 구현 계획

> 헥사고날 아키텍처 + DDD 기반. 서비스 함수는 인터페이스(UseCase)로만 노출.

---

## 1. 패키지 구조

```
com.ject.vs
├── common
│   └── domain
│       ├── BaseEntity.java          # @MappedSuperclass — id 공통 정의
│       ├── BaseTimeEntity.java      # extends BaseEntity, implements TimeTrackable
│       └── TimeTrackable.java       # interface — createdAt/updatedAt 계약
│
├── vote                             # Vote Bounded Context
│   ├── domain
│   │   ├── Vote.java
│   │   └── VoteParticipation.java
│   └── port
│       └── out
│           ├── VoteRepository.java
│           └── VoteParticipationRepository.java
│   └── infrastructure
│       └── persistence
│           ├── VoteJpaRepository.java
│           └── VoteParticipationJpaRepository.java
│
└── chat                             # Chat Bounded Context
    ├── domain
    │   ├── ChatMessage.java
    │   └── ChatRoomUnread.java
    ├── port
    │   ├── in
    │   │   ├── ChatQueryUseCase.java     # inbound port (interface)
    │   │   └── ChatCommandUseCase.java   # inbound port (interface)
    │   └── out
    │       ├── ChatMessagePort.java      # outbound port (interface)
    │       └── ChatRoomUnreadPort.java   # outbound port (interface)
    ├── application
    │   ├── ChatQueryService.java         # implements ChatQueryUseCase
    │   └── ChatCommandService.java       # implements ChatCommandUseCase
    └── infrastructure
        ├── persistence
        │   ├── ChatMessageJpaRepository.java
        │   ├── ChatMessagePersistenceAdapter.java   # implements ChatMessagePort
        │   ├── ChatRoomUnreadJpaRepository.java
        │   └── ChatRoomUnreadPersistenceAdapter.java # implements ChatRoomUnreadPort
        └── web
            └── ChatController.java
│
└── config
    ├── WebSocketConfig.java             # STOMP 엔드포인트 및 브로커 설정
    └── WebSocketAuthInterceptor.java    # STOMP CONNECT 시 JWT 인증
```

---

## 2. 공통 기반

상속 계층:
```
BaseEntity (id)
    └── BaseTimeEntity (id + createdAt + updatedAt)  implements TimeTrackable
```

엔티티는 필요에 따라 둘 중 하나를 상속:
- 시간 추적 불필요 → `extends BaseEntity`
- 시간 추적 필요 → `extends BaseTimeEntity`

### BaseEntity
```java
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

### TimeTrackable
createdAt / updatedAt 계약 인터페이스. 서비스 레이어에서 타입 제약 또는 공통 처리 시 활용.

```java
public interface TimeTrackable {
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
```

### BaseTimeEntity
`@EntityListeners`와 Auditing 어노테이션을 한 곳에 정의. 각 엔티티는 상속만으로 적용.

```java
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity extends BaseEntity implements TimeTrackable {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

```java
// 시간 추적 불필요
public class Vote extends BaseEntity { ... }

// 시간 추적 필요
public class ChatMessage extends BaseTimeEntity { ... }
```

---

## 3. 단계별 구현

---

### STEP 1 — 공통 기반 + Vote Bounded Context

**목표:** 채팅 도메인이 참조할 Vote, VoteParticipation 뼈대 구성

**구현 파일**

| 파일 | 내용 |
|------|------|
| `common/domain/BaseEntity.java` | id 공통 |
| `common/domain/TimeTrackable.java` | 시간 추적 인터페이스 |
| `vote/domain/Vote.java` | extends BaseEntity, id만 관리 |
| `vote/domain/VoteParticipation.java` | extends BaseEntity, voteId + userId |
| `vote/port/out/VoteRepository.java` | `existsById(Long voteId)` |
| `vote/port/out/VoteParticipationRepository.java` | `existsByVoteIdAndUserId(Long, Long)` |
| `vote/infrastructure/persistence/VoteJpaRepository.java` | JPA 구현 |
| `vote/infrastructure/persistence/VoteParticipationJpaRepository.java` | JPA 구현 |
| `db/migration/V2__chat_schema.sql` | vote, vote_participation, chat_message, chat_room_unread |

**VoteParticipation 도메인 핵심 함수**
```java
// 도메인 팩토리
public static VoteParticipation of(Long voteId, Long userId) { ... }
```

**VoteParticipationRepository 인터페이스 (outbound port)**
```java
public interface VoteParticipationRepository {
    boolean existsByVoteIdAndUserId(Long voteId, Long userId);
    VoteParticipation save(VoteParticipation participation);
}
```

**테스트**
- `VoteTest`: 도메인 객체 생성 검증
- `VoteParticipationTest`: `of()` 팩토리 검증
- `VoteParticipationJpaRepositoryTest`: `@DataJpaTest` — 저장 / 존재 여부 조회

---

### STEP 2 — Chat Domain

**목표:** ChatMessage, ChatRoomUnread 도메인 객체 설계

**구현 파일**

| 파일 | 내용 |
|------|------|
| `chat/domain/ChatMessage.java` | extends BaseEntity, implements TimeTrackable |
| `chat/domain/ChatRoomUnread.java` | 복합 PK (userId + voteId), BaseEntity 미사용 |

**ChatMessage 도메인 핵심**
```java
@Entity
public class ChatMessage extends BaseTimeEntity {
    private Long voteId;
    private Long senderId;

    @Column(nullable = false)
    private String content;

    // createdAt = sentAt (BaseTimeEntity에서 자동 주입)

    // 팩토리
    public static ChatMessage of(Long voteId, Long senderId, String content) { ... }

    // 도메인 검증
    public boolean isBlank() { return content.isBlank(); }
}
```

**ChatRoomUnread 도메인 핵심**
```java
@Entity
public class ChatRoomUnread {
    @EmbeddedId
    private ChatRoomUnreadId id;   // userId + voteId 복합 PK

    private Long lastReadMessageId;
    private LocalDateTime lastReadAt;

    public static ChatRoomUnread of(Long userId, Long voteId, Long lastReadMessageId) { ... }
    public void updateLastRead(Long messageId) { ... }
}
```

**테스트**
- `ChatMessageTest`: `of()`, `isBlank()` 검증
- `ChatRoomUnreadTest`: `updateLastRead()` 상태 변이 검증

---

### STEP 3 — Outbound Ports + Persistence Adapters

**목표:** DB I/O 인터페이스 정의 및 JPA 구현

**ChatMessagePort (outbound port)**
```java
public interface ChatMessagePort {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findByVoteIdWithCursor(Long voteId, Long cursor, int size);
    Optional<ChatMessage> findLatestByVoteId(Long voteId);
    long countByVoteIdAfter(Long voteId, Long lastReadMessageId);
}
```

**ChatRoomUnreadPort (outbound port)**
```java
public interface ChatRoomUnreadPort {
    void upsert(ChatRoomUnread unread);
    Optional<ChatRoomUnread> findByUserIdAndVoteId(Long userId, Long voteId);
}
```

**구현 파일**

| 파일 | 내용 |
|------|------|
| `chat/port/out/ChatMessagePort.java` | outbound port |
| `chat/port/out/ChatRoomUnreadPort.java` | outbound port |
| `chat/infrastructure/persistence/ChatMessageJpaRepository.java` | Spring Data JPA |
| `chat/infrastructure/persistence/ChatMessagePersistenceAdapter.java` | implements ChatMessagePort |
| `chat/infrastructure/persistence/ChatRoomUnreadJpaRepository.java` | Spring Data JPA |
| `chat/infrastructure/persistence/ChatRoomUnreadPersistenceAdapter.java` | implements ChatRoomUnreadPort |

**테스트**
- `ChatMessagePersistenceAdapterTest`: `@DataJpaTest` — 커서 페이지네이션, 최신 메시지 조회
- `ChatRoomUnreadPersistenceAdapterTest`: `@DataJpaTest` — upsert 멱등성 검증

---

### STEP 4 — Inbound Ports + Application Services

**목표:** UseCase 인터페이스 정의 + 비즈니스 로직 구현

**ChatQueryUseCase (inbound port)**
```java
public interface ChatQueryUseCase {
    ChatListResult getChatList(Long userId, VoteStatus status);
    ChatRoomResult getChatRoom(Long voteId);
    GaugeResult getGauge(Long voteId);
    MessagePageResult getMessages(Long voteId, Long cursor, int size);
}
```

**ChatCommandUseCase (inbound port)**
```java
public interface ChatCommandUseCase {
    MessageResult sendMessage(Long voteId, Long senderId, String content);
    void markAsRead(Long voteId, Long userId, Long lastReadMessageId);
}
```

**ChatCommandService 핵심 로직**
```java
@Service
public class ChatCommandService implements ChatCommandUseCase {

    @Override
    public MessageResult sendMessage(Long voteId, Long senderId, String content) {
        // 1. 투표 참여 여부 검증
        if (!voteParticipationRepository.existsByVoteIdAndUserId(voteId, senderId))
            throw new ChatForbiddenException();
        // 2. 공백 검증 (도메인 함수 위임)
        ChatMessage message = ChatMessage.of(voteId, senderId, content);
        if (message.isBlank()) throw new InvalidMessageException();
        // 3. 저장 + WebSocket broadcast
        ChatMessage saved = chatMessagePort.save(message);
        messagingTemplate.convertAndSend("/topic/chat/" + voteId, toPayload(saved));
        return MessageResult.from(saved);
    }
}
```

**구현 파일**

| 파일 | 내용 |
|------|------|
| `chat/port/in/ChatQueryUseCase.java` | inbound port |
| `chat/port/in/ChatCommandUseCase.java` | inbound port |
| `chat/application/ChatQueryService.java` | implements ChatQueryUseCase |
| `chat/application/ChatCommandService.java` | implements ChatCommandUseCase |

**테스트**
- `ChatCommandServiceTest`: Mockito로 port mocking — 참여 검증 실패 / 성공 / 공백 메시지 케이스
- `ChatQueryServiceTest`: 목록 정렬, 커서 페이지네이션, unreadCount 계산 검증

---

### STEP 5 — Presentation Layer (REST)

**목표:** REST Controller 구현

**구현 파일**

| 파일 | 내용 |
|------|------|
| `chat/infrastructure/web/ChatController.java` | REST — UseCase 주입, DTO 변환 |

**ChatController 구조**
```java
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatQueryUseCase chatQueryUseCase;
    private final ChatCommandUseCase chatCommandUseCase;

    @GetMapping
    public ChatListResponse getChatList(@AuthenticationPrincipal Long userId,
                                        @RequestParam VoteStatus status) { ... }

    @GetMapping("/{voteId}")
    public ChatRoomResponse getChatRoom(@PathVariable Long voteId) { ... }

    @GetMapping("/{voteId}/gauge")
    public GaugeResponse getGauge(@PathVariable Long voteId) { ... }

    @GetMapping("/{voteId}/messages")
    public MessagePageResponse getMessages(@PathVariable Long voteId,
                                           @RequestParam(required = false) Long cursor,
                                           @RequestParam(defaultValue = "30") int size) { ... }

    @PostMapping("/{voteId}/messages")
    public MessageResponse sendMessage(@PathVariable Long voteId,
                                       @AuthenticationPrincipal Long userId,
                                       @RequestBody SendMessageRequest request) { ... }

    @PostMapping("/{voteId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long voteId,
                           @AuthenticationPrincipal Long userId,
                           @RequestBody MarkAsReadRequest request) { ... }
}
```

**테스트**
- `ChatControllerTest`: `@WebMvcTest` — 각 엔드포인트 요청/응답 검증, 401/403 케이스

---

### STEP 6 — WebSocket 구현

**목표:** STOMP 기반 실시간 메시지 수신/전송 + unreadCount 갱신

---

#### 6-1. WebSocketConfig

STOMP 엔드포인트 및 메시지 브로커 설정.

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();           // SockJS fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");   // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 발행 prefix
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor); // JWT 인증 인터셉터 등록
    }
}
```

---

#### 6-2. WebSocketAuthInterceptor

STOMP CONNECT frame 수신 시 `Authorization` 헤더에서 JWT를 추출해 인증 처리.  
이후 메시지에서 `accessor.getUser()`로 인증 정보 참조 가능.

```java
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                Long userId = jwtProvider.extractUserId(token.substring(7));
                accessor.setUser(() -> String.valueOf(userId));  // Principal 설정
            }
        }
        return message;
    }
}
```

---

#### 6-3. broadcast 흐름

메시지 전송은 REST를 통해 이루어지며, `ChatCommandService` 내부에서 `SimpMessagingTemplate`으로 broadcast.  
`@MessageMapping` 핸들러(`ChatWebSocketHandler`)는 불필요.

```
REST POST /api/chats/{voteId}/messages  { "content": "..." }
        │
        ▼
  ChatController.sendMessage()
        │
        ▼
  ChatCommandService.sendMessage()
    ├─ VoteParticipationRepository: 참여 여부 검증
    ├─ ChatMessage.of(): 도메인 객체 생성 + 공백 검증
    ├─ ChatMessagePort.save(): DB 저장
    ├─ SimpMessagingTemplate → /topic/chat/{voteId}
    │     payload: { messageId, content, sentAt, senderNickname, senderVoteOption, isMine }
    └─ SimpMessagingTemplate → /topic/chat/{voteId}/unread
          payload: { unreadCount }
```

---

#### 6-4. 구현 파일

| 파일 | 내용 |
|------|------|
| `config/WebSocketConfig.java` | STOMP 엔드포인트 및 브로커 설정 |
| `config/WebSocketAuthInterceptor.java` | CONNECT 시 JWT 인증, Principal 주입 |

---

#### 6-5. 테스트

- `WebSocketAuthInterceptorTest`: CONNECT 시 유효/무효 토큰 처리 검증
- `ChatWebSocketIntegrationTest`: REST POST 후 `/topic/chat/{voteId}` 수신 검증
  ```java
  // 통합 테스트 흐름 예시
  StompSession session = stompClient.connect(url, headers).get();
  session.subscribe("/topic/chat/1", new TestStompFrameHandler());
  // REST POST로 메시지 전송
  mockMvc.perform(post("/api/chats/1/messages").content(...));
  // WebSocket으로 broadcast된 payload 검증
  ```

---

## 4. Flyway 마이그레이션

| 파일 | 내용 |
|------|------|
| `V1__init_schema.sql` | 기존 (users, token) |
| `V2__chat_schema.sql` | vote, vote_participation, chat_message, chat_room_unread, User 컬럼 추가 |

---

## 5. 테스트 전략 요약

| 레이어 | 도구 | 대상 |
|--------|------|------|
| Domain | JUnit 5 (순수 단위) | 팩토리 메서드, 도메인 검증 함수 |
| Application | JUnit 5 + Mockito | UseCase 비즈니스 로직, 예외 케이스 |
| Persistence | `@DataJpaTest` | 쿼리 정합성, 커서 페이지네이션, upsert |
| Controller | `@WebMvcTest` | 요청/응답 직렬화, 인증 필터 |
| WebSocket | `StompClient` 통합 | REST POST → `/topic` 수신 end-to-end |

---

## 6. 구현 순서 요약

```
STEP 1  BaseEntity / BaseTimeEntity / TimeTrackable / Vote BC (도메인 + port + JPA)
STEP 2  ChatMessage / ChatRoomUnread 도메인
STEP 3  Outbound ports + Persistence Adapters
STEP 4  Inbound ports (UseCase) + Application Services
STEP 5  REST Controller
STEP 6  WebSocket 구현
          6-1. WebSocketConfig (STOMP 엔드포인트 + 브로커)
          6-2. WebSocketAuthInterceptor (JWT 인증)
          6-3. broadcast 흐름 (REST → ChatCommandService → SimpMessagingTemplate)
```
