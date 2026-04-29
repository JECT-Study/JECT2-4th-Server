package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.InvalidMessageException;
import com.ject.vs.chat.port.in.MarkAsReadCommand;
import com.ject.vs.chat.port.in.MessageResult;
import com.ject.vs.chat.port.in.SendMessageCommand;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceTest {

    @InjectMocks
    private ChatCommandService chatCommandService;

    @Mock
    private VoteParticipationRepository voteParticipationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomUnreadRepository chatRoomUnreadRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    class sendMessage {

        @Test
        void 미참여자는_ChatForbiddenException이_발생한다() {
            // given
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatCommandService.sendMessage(new SendMessageCommand(1L, 2L, "hello")))
                    .isInstanceOf(ChatForbiddenException.class);
        }

        @Test
        void 공백_내용은_InvalidMessageException이_발생한다() {
            // given
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatCommandService.sendMessage(new SendMessageCommand(1L, 2L, "   ")))
                    .isInstanceOf(InvalidMessageException.class);
        }

        @Test
        void 정상_메시지는_저장되고_MessageResult를_반환한다() {
            // given
            given(voteParticipationRepository.existsByVoteIdAndUserId(1L, 2L)).willReturn(true);
            ChatMessage savedMessage = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
            given(userRepository.findById(2L)).willReturn(Optional.empty());

            // when
            MessageResult result = chatCommandService.sendMessage(new SendMessageCommand(1L, 2L, "hello"));

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("hello");
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }
    }

    @Nested
    class markAsRead {

        @Test
        void 신규인_경우_새로_생성된다() {
            // given
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(1L, 10L)).willReturn(Optional.empty());

            // when
            chatCommandService.markAsRead(new MarkAsReadCommand(10L, 1L, 5L));

            // then
            verify(chatRoomUnreadRepository).save(any(ChatRoomUnread.class));
        }

        @Test
        void 기존_있으면_updateLastRead가_호출된다() {
            // given
            ChatRoomUnread existing = ChatRoomUnread.of(1L, 10L, 3L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(1L, 10L)).willReturn(Optional.of(existing));

            // when
            chatCommandService.markAsRead(new MarkAsReadCommand(10L, 1L, 20L));

            // then
            assertThat(existing.getLastReadMessageId()).isEqualTo(20L);
            verify(chatRoomUnreadRepository).save(existing);
        }
    }
}
