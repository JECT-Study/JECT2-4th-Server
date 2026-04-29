package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageEventListenerTest {

    @InjectMocks
    private ChatMessageEventListener listener;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoteParticipationQueryUseCase voteParticipationQueryUseCase;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Nested
    class handle {

        @Test
        void 메시지를_채팅방_토픽으로_broadcast한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userRepository.findById(2L)).willReturn(Optional.empty());
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), any(Object.class));
        }

        @Test
        void 발신자를_찾을_수_없으면_닉네임을_unknown으로_설정한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userRepository.findById(2L)).willReturn(Optional.empty());
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), captor.capture());
            assertThat(captor.getValue().toString()).contains("unknown");
        }

        @Test
        void 발신자가_존재하면_sub을_닉네임으로_사용한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            User sender = User.createWithSub("슈퍼강아지_485");
            given(userRepository.findById(2L)).willReturn(Optional.of(sender));
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), captor.capture());
            assertThat(captor.getValue().toString()).contains("슈퍼강아지_485");
        }
    }

    @Nested
    class broadcastUnreadCount {

        @Test
        void 참여자별로_unread_count를_개인_채널로_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userRepository.findById(2L)).willReturn(Optional.empty());
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of(3L));
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(3L, 1L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByVoteIdAndIdGreaterThan(1L, 0L)).willReturn(5L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSendToUser(eq("3"), eq("/topic/chat/1/unread"), captor.capture());
            assertThat(captor.getValue().get("unreadCount")).isEqualTo(5L);
        }

        @Test
        void 읽음_기록이_있으면_lastReadMessageId_이후_메시지_수를_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userRepository.findById(2L)).willReturn(Optional.empty());
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of(3L));
            ChatRoomUnread unread = ChatRoomUnread.of(3L, 1L, 10L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(3L, 1L)).willReturn(Optional.of(unread));
            given(chatMessageRepository.countByVoteIdAndIdGreaterThan(1L, 10L)).willReturn(2L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSendToUser(eq("3"), eq("/topic/chat/1/unread"), captor.capture());
            assertThat(captor.getValue().get("unreadCount")).isEqualTo(2L);
        }
    }
}
