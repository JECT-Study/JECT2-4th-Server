package com.ject.vs.chat.adapter.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
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
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatMessageEventListener listener;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteOptionRepository voteOptionRepository;

    @Autowired
    private VoteParticipationRepository voteParticipationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<StompSession> sessions = new java.util.concurrent.CopyOnWriteArrayList<>();

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
    void 이벤트_발생_후_topic_chat_voteId로_실제_STOMP_메시지를_수신한다() throws Exception {
        // given
        TestFixture fixture = createFixture();
        StompSession session = connectAnonymously();
        BlockingQueue<MessageResult> messages = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/" + fixture.voteId(), handler(MessageResult.class, messages));

        // when
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.of(fixture.voteId(), fixture.senderId(), "hello websocket"));
        listener.handle(new com.ject.vs.chat.domain.event.ChatMessageSentEvent(message));

        // then
        MessageResult received = messages.poll(3, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.messageId()).isEqualTo(message.getId());
        assertThat(received.content()).isEqualTo("hello websocket");
        assertThat(received.senderNickname()).isEqualTo(""); // 프로필 미설정 사용자 (정식 스펙상 User# 플레이스홀더 없음)
        assertThat(received.isMine()).isFalse();
    }

    @Test
    void 인증_쿠키로_연결하면_user_destination_unreadCount를_사용자별로_분리해_수신한다() throws Exception {
        // given
        TestFixture fixture = createFixture();
        ChatMessage previousMessage = chatMessageRepository.saveAndFlush(ChatMessage.of(fixture.voteId(), fixture.senderId(), "previous message"));
        chatRoomUnreadRepository.saveAndFlush(ChatRoomUnread.of(fixture.receiverId(), fixture.voteId(), previousMessage.getId()));
        StompSession receiverSession = connectWithAccessToken(fixture.receiverId());
        BlockingQueue<UnreadPayload> receiverUnread = new LinkedBlockingQueue<>();
        receiverSession.subscribe("/user/topic/chat/" + fixture.voteId() + "/unread", handler(UnreadPayload.class, receiverUnread));

        StompSession senderSession = connectWithAccessToken(fixture.senderId());
        BlockingQueue<UnreadPayload> senderUnread = new LinkedBlockingQueue<>();
        senderSession.subscribe("/user/topic/chat/" + fixture.voteId() + "/unread", handler(UnreadPayload.class, senderUnread));

        // when
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.of(fixture.voteId(), fixture.senderId(), "unread websocket"));
        listener.handle(new com.ject.vs.chat.domain.event.ChatMessageSentEvent(message));

        // then
        UnreadPayload received = receiverUnread.poll(3, TimeUnit.SECONDS);
        assertThat(received).isEqualTo(new UnreadPayload(fixture.voteId(), 1L));
        UnreadPayload senderPayload = senderUnread.poll(3, TimeUnit.SECONDS);
        assertThat(senderPayload).isEqualTo(new UnreadPayload(fixture.voteId(), 2L));
    }

    @Test
    void voteId가_다른_topic_구독자는_메시지를_수신하지_않는다() throws Exception {
        // given
        TestFixture fixture = createFixture();
        Vote otherVote = voteRepository.saveAndFlush(Vote.create(VoteType.GENERAL, "other", null, "thumb", null, Duration.ofHours(1), Clock.systemUTC()));
        StompSession session = connectAnonymously();
        BlockingQueue<MessageResult> targetMessages = new LinkedBlockingQueue<>();
        BlockingQueue<MessageResult> otherMessages = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/" + fixture.voteId(), handler(MessageResult.class, targetMessages));
        session.subscribe("/topic/chat/" + otherVote.getId(), handler(MessageResult.class, otherMessages));

        // when
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.of(fixture.voteId(), fixture.senderId(), "only target vote"));
        listener.handle(new com.ject.vs.chat.domain.event.ChatMessageSentEvent(message));

        // then
        assertThat(targetMessages.poll(3, TimeUnit.SECONDS)).isNotNull();
        assertThat(otherMessages.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    private TestFixture createFixture() {
        User sender = userRepository.saveAndFlush(User.createWithEmail("sender-" + System.nanoTime() + "@test.com"));
        User receiver = userRepository.saveAndFlush(User.createWithEmail("receiver-" + System.nanoTime() + "@test.com"));
        Vote vote = voteRepository.saveAndFlush(Vote.create(VoteType.GENERAL, "chat vote", null, "thumb", null, Duration.ofHours(1), Clock.systemUTC()));
        VoteOption option = voteOptionRepository.saveAndFlush(VoteOption.of(vote, "A", 1));
        voteParticipationRepository.saveAndFlush(VoteParticipation.ofMember(vote.getId(), sender.getId(), option.getId()));
        voteParticipationRepository.saveAndFlush(VoteParticipation.ofMember(vote.getId(), receiver.getId(), option.getId()));
        return new TestFixture(vote.getId(), sender.getId(), receiver.getId());
    }

    private StompSession connectAnonymously() throws Exception {
        StompSession session = stompClient().connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
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
