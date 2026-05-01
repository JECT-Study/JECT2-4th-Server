package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.UnreadPayload;
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
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(1L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), any(Object.class));
        }

        @Test
        void 닉네임은_userId_기반_플레이스홀더로_설정된다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(1L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<MessageResult> captor = ArgumentCaptor.forClass(MessageResult.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), captor.capture());
            assertThat(captor.getValue().senderNickname()).isEqualTo("User#2");
        }
    }

    @Nested
    class broadcastUnreadCount {

        @Test
        void 읽음_기록이_없는_참여자에게는_전체_메시지_수를_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of(3L));
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(3L, 1L)).willReturn(Optional.empty());
            given(chatMessageRepository.countByVoteId(1L)).willReturn(5L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<UnreadPayload> captor = ArgumentCaptor.forClass(UnreadPayload.class);
            verify(messagingTemplate).convertAndSendToUser(eq("3"), eq("/topic/chat/1/unread"), captor.capture());
            assertThat(captor.getValue().unreadCount()).isEqualTo(5L);
        }

        @Test
        void 읽음_기록이_있으면_lastReadMessageId_이후_메시지_수를_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of(3L));
            ChatRoomUnread unread = ChatRoomUnread.of(3L, 1L, 10L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(3L, 1L)).willReturn(Optional.of(unread));
            given(chatMessageRepository.countByVoteId(1L)).willReturn(10L);
            given(chatMessageRepository.countByVoteIdAndIdGreaterThan(1L, 10L)).willReturn(2L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<UnreadPayload> captor = ArgumentCaptor.forClass(UnreadPayload.class);
            verify(messagingTemplate).convertAndSendToUser(eq("3"), eq("/topic/chat/1/unread"), captor.capture());
            assertThat(captor.getValue().unreadCount()).isEqualTo(2L);
        }
    }
}
