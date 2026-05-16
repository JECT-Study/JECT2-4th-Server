package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.UnreadPayload;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;

import static org.mockito.Mockito.mock;
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
import static org.mockito.Mockito.never;
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
    private VoteQueryUseCase voteQueryUseCase;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Nested
    class handle {

        @Test
        void 메시지를_채팅방_토픽으로_broadcast한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userQueryUseCase.getUser(2L)).willReturn(mock(User.class));
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(1L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), any(Object.class));
        }

        @Test
        void 채팅_broadcast_payload는_메시지_정보를_담고_수신자_관점_isMine은_false다() {
            // given
            ChatMessage message = ChatMessage.of(7L, 2L, "hello payload");

            User sender = mock(User.class);
            given(sender.getNickname()).willReturn("슈퍼강아지_485");
            given(sender.getImageColor()).willReturn(ImageColor.GREEN);
            given(userQueryUseCase.getUser(2L)).willReturn(sender);

            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(7L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(7L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            ArgumentCaptor<MessageResult> captor = ArgumentCaptor.forClass(MessageResult.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/7"), captor.capture());
            MessageResult payload = captor.getValue();
            assertThat(payload.content()).isEqualTo("hello payload");
            assertThat(payload.senderNickname()).isEqualTo("슈퍼강아지_485");
            assertThat(payload.isMine()).isFalse();
        }

        @Test
        void voteId별로_다른_topic에_broadcast한다() {
            // given
            ChatMessage message = ChatMessage.of(2L, 9L, "vote 2 message");
            given(userQueryUseCase.getUser(9L)).willReturn(mock(User.class));
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(2L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(2L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/2"), any(MessageResult.class));
            verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat/1"), any(Object.class));
        }

    }

    @Nested
    class broadcastUnreadCount {

        @Test
        void 읽음_기록이_없는_참여자에게는_전체_메시지_수를_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(userQueryUseCase.getUser(2L)).willReturn(mock(User.class));
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
            given(userQueryUseCase.getUser(2L)).willReturn(mock(User.class));
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

        @Test
        void 참여자가_여러_명이면_각_user_destination으로_개별_unreadCount를_전송한다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of(3L, 4L));
            given(chatMessageRepository.countByVoteId(1L)).willReturn(7L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(3L, 1L)).willReturn(Optional.empty());
            ChatRoomUnread unread = ChatRoomUnread.of(4L, 1L, 20L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(4L, 1L)).willReturn(Optional.of(unread));
            given(chatMessageRepository.countByVoteIdAndIdGreaterThan(1L, 20L)).willReturn(1L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("3"),
                    eq("/topic/chat/1/unread"),
                    eq(new UnreadPayload(1L, 7L))
            );
            verify(messagingTemplate).convertAndSendToUser(
                    eq("4"),
                    eq("/topic/chat/1/unread"),
                    eq(new UnreadPayload(1L, 1L))
            );
        }

        @Test
        void 참여자가_없으면_개인_unreadCount는_전송하지_않는다() {
            // given
            ChatMessage message = ChatMessage.of(1L, 2L, "hello");
            given(voteParticipationQueryUseCase.findAllUserIdsByVoteId(1L)).willReturn(List.of());
            given(chatMessageRepository.countByVoteId(1L)).willReturn(0L);

            // when
            listener.handle(new ChatMessageSentEvent(message));

            // then
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(UnreadPayload.class));
        }
    }
}
