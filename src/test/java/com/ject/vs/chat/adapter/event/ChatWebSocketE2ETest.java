package com.ject.vs.chat.adapter.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.chat.adapter.web.dto.SendMessageRequest;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.UnreadPayload;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.util.CookieUtil;
import com.ject.vs.util.JwtProvider;
import com.ject.vs.vote.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteOptionRepository voteOptionRepository;

    @Autowired
    private VoteParticipationRepository voteParticipationRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<StompSession> sessions = new CopyOnWriteArrayList<>();

    @AfterEach
    void disconnectSessions() {
        sessions.forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        sessions.clear();
    }

    @Test
    void REST로_채팅을_전송하면_commit_이후_STOMP_채팅과_개인_unread를_수신한다() throws Exception {
        // given
        TestFixture fixture = createFixture();
        StompSession receiverSession = connectWithAccessToken(fixture.receiverId());
        BlockingQueue<MessageResult> chatMessages = new LinkedBlockingQueue<>();
        BlockingQueue<UnreadPayload> unreadMessages = new LinkedBlockingQueue<>();
        receiverSession.subscribe("/topic/chat/" + fixture.voteId(), handler(MessageResult.class, chatMessages));
        receiverSession.subscribe("/user/topic/chat/" + fixture.voteId() + "/unread", handler(UnreadPayload.class, unreadMessages));

        HttpEntity<SendMessageRequest> request = new HttpEntity<>(
                new SendMessageRequest("hello e2e websocket"),
                authenticatedJsonHeaders(fixture.senderId())
        );

        // when
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chats/" + fixture.voteId() + "/messages",
                HttpMethod.POST,
                request,
                String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        MessageResult chatPayload = chatMessages.poll(5, TimeUnit.SECONDS);
        assertThat(chatPayload).isNotNull();
        assertThat(chatPayload.content()).isEqualTo("hello e2e websocket");
        assertThat(chatPayload.senderNickname()).isEqualTo(""); // 프로필 미설정 사용자 (정식 스펙상 User# 플레이스홀더 없음)
        assertThat(chatPayload.isMine()).isFalse();

        UnreadPayload unreadPayload = unreadMessages.poll(5, TimeUnit.SECONDS);
        assertThat(unreadPayload).isEqualTo(new UnreadPayload(fixture.voteId(), 1L));
    }

    private TestFixture createFixture() {
        User sender = userRepository.saveAndFlush(User.createWithEmail("e2e-sender-" + System.nanoTime() + "@test.com"));
        User receiver = userRepository.saveAndFlush(User.createWithEmail("e2e-receiver-" + System.nanoTime() + "@test.com"));
        Vote vote = voteRepository.saveAndFlush(Vote.create(VoteType.GENERAL, "e2e chat vote", null, "thumb", null, Duration.ofHours(1), Clock.systemUTC()));
        VoteOption option = voteOptionRepository.saveAndFlush(VoteOption.of(vote, "A", 1));
        voteParticipationRepository.saveAndFlush(VoteParticipation.ofMember(vote.getId(), sender.getId(), option.getId()));
        voteParticipationRepository.saveAndFlush(VoteParticipation.ofMember(vote.getId(), receiver.getId(), option.getId()));
        return new TestFixture(vote.getId(), sender.getId(), receiver.getId());
    }

    private HttpHeaders authenticatedJsonHeaders(Long userId) {
        String csrfToken = UUID.randomUUID().toString();
        String accessToken = jwtProvider.createAccessToken(userId).tokenValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, CookieUtil.CookieType.ACCESS_TOKEN + "=" + accessToken + "; XSRF-TOKEN=" + csrfToken);
        headers.add("X-XSRF-TOKEN", csrfToken);
        return headers;
    }

    private StompSession connectWithAccessToken(Long userId) throws Exception {
        String token = jwtProvider.createAccessToken(userId).tokenValue();
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.add("Cookie", CookieUtil.CookieType.ACCESS_TOKEN + "=" + token);
        StompSession session = stompClient().connectAsync("http://localhost:" + port + "/ws", httpHeaders, new StompHeaders(), new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private WebSocketStompClient stompClient() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
        return stompClient;
    }

    private <T> StompFrameHandler handler(Class<T> payloadType, BlockingQueue<T> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(payloadType.cast(payload));
            }
        };
    }

    private record TestFixture(Long voteId, Long senderId, Long receiverId) {
    }
}
