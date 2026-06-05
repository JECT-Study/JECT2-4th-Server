package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.InvalidMessageException;
import com.ject.vs.chat.port.in.dto.ChatListItemResult;
import com.ject.vs.chat.port.in.dto.MarkAsReadCommand;
import com.ject.vs.chat.port.in.dto.MessagePageResult;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.SendMessageCommand;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.domain.VoteOptionCode;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;

import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

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
    class sendMessage {

        @Test
        void 미참여자는_ChatForbiddenException이_발생한다() {
            // given
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage(new SendMessageCommand(1L, 2L, "hello")))
                    .isInstanceOf(ChatForbiddenException.class);
        }

        @Test
        void 공백_내용은_InvalidMessageException이_발생한다() {
            // given
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage(new SendMessageCommand(1L, 2L, "   ")))
                    .isInstanceOf(InvalidMessageException.class);
        }

        @Test
        void 정상_메시지는_저장되고_MessageResult를_반환한다() {
            // given
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            ChatMessage savedMessage = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);

            User sender = mock(User.class);
            given(sender.getNickname()).willReturn("테스트유저");
            given(sender.getImageColor()).willReturn(ImageColor.GREEN);
            given(userQueryUseCase.getUser(2L)).willReturn(sender);
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.of(VoteOptionCode.A));

            // when
            MessageResult result = chatService.sendMessage(new SendMessageCommand(1L, 2L, "hello"));

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("hello");
            assertThat(result.senderNickname()).isEqualTo("테스트유저");
            assertThat(result.senderVoteOption()).isEqualTo(VoteOptionCode.A);
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
            chatService.markAsRead(new MarkAsReadCommand(10L, 1L, 5L));

            // then
            verify(chatRoomUnreadRepository).save(any(ChatRoomUnread.class));
        }

        @Test
        void 기존_있으면_updateLastRead가_호출된다() {
            // given
            ChatRoomUnread existing = ChatRoomUnread.of(1L, 10L, 3L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(1L, 10L)).willReturn(Optional.of(existing));

            // when
            chatService.markAsRead(new MarkAsReadCommand(10L, 1L, 20L));

            // then
            assertThat(existing.getLastReadMessageId()).isEqualTo(20L);
        }
    }

    @Nested
    class getChatList {

        @Test
        void 최근_메시지_시간_기준_내림차순으로_정렬한다() {
            // given
            Long userId = 1L;
            given(voteParticipationQueryUseCase.findAllVoteIdsByUserId(userId)).willReturn(List.of(10L, 20L, 30L));
            given(voteQueryUseCase.findAllVoteIdsByStatus(List.of(10L, 20L, 30L), VoteStatus.ONGOING))
                    .willReturn(List.of(10L, 20L, 30L));

            Instant endAt = Instant.parse("2026-12-31T00:00:00Z");
            given(voteQueryUseCase.getVoteChatSummary(10L))
                    .willReturn(new VoteQueryUseCase.VoteChatSummary(10L, "t10", null, VoteStatus.ONGOING, endAt, "A", "B"));
            given(voteQueryUseCase.getVoteChatSummary(20L))
                    .willReturn(new VoteQueryUseCase.VoteChatSummary(20L, "t20", null, VoteStatus.ONGOING, endAt, "A", "B"));
            given(voteQueryUseCase.getVoteChatSummary(30L))
                    .willReturn(new VoteQueryUseCase.VoteChatSummary(30L, "t30", null, VoteStatus.ONGOING, endAt, "A", "B"));

            given(voteParticipationQueryUseCase.countParticipantsByVoteId(any())).willReturn(1L);
            given(chatRoomUnreadRepository.findByIdUserIdAndIdVoteId(eq(userId), any())).willReturn(Optional.empty());
            given(chatMessageRepository.countByVoteId(any())).willReturn(0L);

            ChatMessage oldMsg = mock(ChatMessage.class);
            ChatMessage midMsg = mock(ChatMessage.class);
            ChatMessage newMsg = mock(ChatMessage.class);

            given(oldMsg.getContent()).willReturn("old");
            given(midMsg.getContent()).willReturn("mid");
            given(newMsg.getContent()).willReturn("new");
            given(oldMsg.getCreatedAt()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
            given(midMsg.getCreatedAt()).willReturn(Instant.parse("2026-01-02T00:00:00Z"));
            given(newMsg.getCreatedAt()).willReturn(Instant.parse("2026-01-03T00:00:00Z"));

            given(chatMessageRepository.findFirstByVoteIdOrderByIdDesc(10L)).willReturn(Optional.of(oldMsg));
            given(chatMessageRepository.findFirstByVoteIdOrderByIdDesc(20L)).willReturn(Optional.of(midMsg));
            given(chatMessageRepository.findFirstByVoteIdOrderByIdDesc(30L)).willReturn(Optional.of(newMsg));

            // when
            List<ChatListItemResult> result = chatService.getChatList(userId, VoteStatus.ONGOING);

            // then
            assertThat(result).extracting(ChatListItemResult::voteId).containsExactly(30L, 20L, 10L);
            assertThat(result).extracting(ChatListItemResult::lastMessageAt)
                    .containsExactly(
                            Instant.parse("2026-01-03T00:00:00Z"),
                            Instant.parse("2026-01-02T00:00:00Z"),
                            Instant.parse("2026-01-01T00:00:00Z"));
        }
    }

    @Nested
    class getMessages {

        @Test
        void cursor가_null이면_최신부터_조회한다() {
            // given
            ChatMessage msg = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.findAllByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            User sender = mock(User.class);
            given(sender.getNickname()).willReturn("");
            given(sender.getImageColor()).willReturn(ImageColor.GREEN);
            given(userQueryUseCase.getUser(2L)).willReturn(sender);
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.empty());

            // when
            MessagePageResult result = chatService.getMessages(1L, 2L, null, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.messages().getFirst().senderVoteOption()).isNull();
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findAllByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class));
        }

        @Test
        void cursor가_있으면_cursor_이전_메시지를_조회한다() {
            // given
            ChatMessage msg = ChatMessage.of(1L, 2L, "hello");
            given(chatMessageRepository.findAllByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            User sender = mock(User.class);
            given(sender.getNickname()).willReturn("");
            given(sender.getImageColor()).willReturn(ImageColor.GREEN);
            given(userQueryUseCase.getUser(2L)).willReturn(sender);
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.of(VoteOptionCode.B));

            // when
            MessagePageResult result = chatService.getMessages(1L, 2L, 100L, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findAllByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class));
        }
    }
}
