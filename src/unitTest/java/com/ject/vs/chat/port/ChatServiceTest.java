package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.*;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.ChatMessageNotFoundException;
import com.ject.vs.chat.exception.InvalidMessageException;
import com.ject.vs.chat.port.in.dto.*;
import com.ject.vs.vote.domain.VoteStatus;

import java.util.Map;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    private ChatMessageReactionRepository chatMessageReactionRepository;

    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Mock
    private ReplyInfoResolver replyInfoResolver;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
            given(userQueryUseCase.getUser(2L)).willReturn(mockUser(2L, "테스트유저", ImageColor.GREEN));

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage(new SendMessageCommand(1L, 2L, "   ")))
                    .isInstanceOf(InvalidMessageException.class);
        }

        @Test
        void 정상_메시지는_저장되고_MessageResult를_반환한다() {
            // given
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            User sender = mockUser(2L, "테스트유저", ImageColor.GREEN);
            ChatMessage savedMessage = ChatMessage.of(1L, sender, "hello");
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(savedMessage);
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
            User sender = mockUser(2L, "", ImageColor.GREEN);
            ChatMessage msg = ChatMessage.of(1L, sender, "hello");
            given(chatMessageRepository.findAllByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.empty());

            // reaction / parent enrichment stubs
            given(chatMessageReactionRepository.countByMessageIds(anyList())).willReturn(List.of());
            given(chatMessageReactionRepository.findMyReactionsByMessageIds(anyList(), any())).willReturn(List.of());
            given(replyInfoResolver.resolveAll(anyList())).willReturn(Map.of());

            // when
            MessagePageResult result = chatService.getMessages(1L, 2L, null, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.messages().getFirst().senderVoteOption()).isNull();
            assertThat(result.messages().getFirst().replyTo()).isNull();
            assertThat(result.messages().getFirst().reactions()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findAllByVoteIdOrderByIdDesc(eq(1L), any(PageRequest.class));
        }

        @Test
        void cursor가_있으면_cursor_이전_메시지를_조회한다() {
            // given
            User sender = mockUser(2L, "", ImageColor.GREEN);
            ChatMessage msg = ChatMessage.of(1L, sender, "hello");
            given(chatMessageRepository.findAllByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class)))
                    .willReturn(List.of(msg));
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.of(VoteOptionCode.B));

            given(chatMessageReactionRepository.countByMessageIds(anyList())).willReturn(List.of());
            given(chatMessageReactionRepository.findMyReactionsByMessageIds(anyList(), any())).willReturn(List.of());
            given(replyInfoResolver.resolveAll(anyList())).willReturn(Map.of());

            // when
            MessagePageResult result = chatService.getMessages(1L, 2L, 100L, 30);

            // then
            assertThat(result.messages()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            verify(chatMessageRepository).findAllByVoteIdAndIdLessThanOrderByIdDesc(eq(1L), eq(100L), any(PageRequest.class));
        }
    }

    @Nested
    class getChatRoom {

        @Test
        void myVoteOption을_포함하여_반환한다() {
            // given
            given(voteQueryUseCase.getVoteChatSummary(1L))
                    .willReturn(new VoteQueryUseCase.VoteChatSummary(1L, "title", null, VoteStatus.ONGOING, Instant.now(), "A", "B"));
            given(voteParticipationQueryUseCase.countParticipantsByVoteId(1L)).willReturn(10L);
            given(voteQueryUseCase.findSelectedOptionCode(1L, 99L)).willReturn(Optional.of(VoteOptionCode.A));

            // when
            var result = chatService.getChatRoom(1L, 99L);

            // then
            assertThat(result.myVoteOption()).isEqualTo(VoteOptionCode.A);
            assertThat(result.voteId()).isEqualTo(1L);
        }
    }

    @Nested
    class reactToMessage {

        @Test
        void 미참여자는_ChatForbiddenException() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(false);

            assertThatThrownBy(() -> chatService.reactToMessage(1L, 2L, 10L, ChatReactionType.THUMBS_UP))
                    .isInstanceOf(ChatForbiddenException.class);
        }

        @Test
        void 자신의_메시지에는_반응할_수_없다() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            ChatMessage ownMsg = ChatMessage.of(1L, mockUser(2L, "me", ImageColor.GREEN), "hello");
            // simulate id
            // since of doesn't set id, we use reflection or just mock find
            given(chatMessageRepository.findById(10L)).willReturn(Optional.of(ownMsg));

            assertThatThrownBy(() -> chatService.reactToMessage(1L, 2L, 10L, ChatReactionType.THUMBS_UP))
                    .isInstanceOf(ChatForbiddenException.class);
        }

        @Test
        void 정상_새_반응_추가() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            ChatMessage msg = ChatMessage.of(1L, mockUser(3L, "other", ImageColor.BLUE), "hi");
            given(chatMessageRepository.findById(10L)).willReturn(Optional.of(msg));
            ChatMessageReaction savedReaction = ChatMessageReaction.of(10L, 2L, ChatReactionType.THUMBS_UP);
            given(chatMessageReactionRepository.findByMessageIdAndUserId(10L, 2L))
                    .willReturn(Optional.empty(), Optional.of(savedReaction));
            given(chatMessageReactionRepository.countByMessageIds(List.of(10L)))
                    .willReturn(List.of(new ReactionCount(10L, ChatReactionType.THUMBS_UP, 1L)));
            given(chatMessageReactionRepository.save(any(ChatMessageReaction.class)))
                    .willReturn(savedReaction);

            ReactionResult result = chatService.reactToMessage(1L, 2L, 10L, ChatReactionType.THUMBS_UP);

            assertThat(result.myReaction()).isEqualTo(ChatReactionType.THUMBS_UP);
            assertThat(result.reactions().get(ChatReactionType.THUMBS_UP)).isEqualTo(1L);
            verify(chatMessageReactionRepository).save(any(ChatMessageReaction.class));
        }

        @Test
        void emoji_null이면_취소() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            ChatMessage msg = ChatMessage.of(1L, mockUser(3L, "other", ImageColor.BLUE), "hi");
            given(chatMessageRepository.findById(10L)).willReturn(Optional.of(msg));
            given(chatMessageReactionRepository.findByMessageIdAndUserId(10L, 2L))
                    .willReturn(Optional.of(ChatMessageReaction.of(10L, 2L, ChatReactionType.THUMBS_DOWN)), Optional.empty());
            given(chatMessageReactionRepository.countByMessageIds(List.of(10L))).willReturn(List.of());

            ReactionResult result = chatService.reactToMessage(1L, 2L, 10L, null);

            assertThat(result.myReaction()).isNull();
            verify(chatMessageReactionRepository).deleteByMessageIdAndUserId(10L, 2L);
        }
    }

    @Nested
    class sendMessageWithReply {

        @Test
        void 정상_답글_전송() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);

            User parentSender = mockUser(99L, "원작성자", ImageColor.YELLOW);
            ChatMessage parent = ChatMessage.of(1L, parentSender, "원문");
            given(chatMessageRepository.findById(100L)).willReturn(Optional.of(parent));

            User sender = mockUser(2L, "답변자", ImageColor.BLUE);
            ChatMessage saved = ChatMessage.of(1L, sender, "답글", parent);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(saved);
            given(userQueryUseCase.getUser(2L)).willReturn(sender);
            given(voteQueryUseCase.findSelectedOptionCode(1L, 2L)).willReturn(Optional.of(VoteOptionCode.B));

            ReplyInfo replyInfo = new ReplyInfo(100L, "원작성자", "원문");
            given(replyInfoResolver.from(parent)).willReturn(replyInfo);

            MessageResult result = chatService.sendMessage(new SendMessageCommand(1L, 2L, "답글입니다", 100L));

            assertThat(result.replyTo()).isEqualTo(replyInfo);
            assertThat(result.replyTo().messageId()).isEqualTo(100L);
            assertThat(result.content()).isEqualTo("답글입니다");
        }

        @Test
        void 다른_투표의_메시지에_답글_불가() {
            given(voteParticipationQueryUseCase.isParticipant(1L, 2L)).willReturn(true);
            User parentSender = mockUser(99L, "원작성자", ImageColor.YELLOW);
            ChatMessage parent = ChatMessage.of(999L, parentSender, "다른투표");
            given(chatMessageRepository.findById(100L)).willReturn(Optional.of(parent));
            given(userQueryUseCase.getUser(2L)).willReturn(mockUser(2L, "답변자", ImageColor.BLUE));

            assertThatThrownBy(() ->
                    chatService.sendMessage(new SendMessageCommand(1L, 2L, "답글", 100L))
            ).isInstanceOf(InvalidMessageException.class);
        }
    }

    private User mockUser(Long id, String nickname, ImageColor imageColor) {
        User user = mock(User.class);
        given(user.getId()).willReturn(id);
        given(user.getNickname()).willReturn(nickname);
        given(user.getImageColor()).willReturn(imageColor);
        return user;
    }
}
