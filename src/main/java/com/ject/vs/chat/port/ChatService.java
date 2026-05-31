package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.InvalidMessageException;
import com.ject.vs.chat.port.in.ChatCommandUseCase;
import com.ject.vs.chat.port.in.ChatQueryUseCase;
import com.ject.vs.chat.port.in.dto.*;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.domain.VoteOptionCode;
import com.ject.vs.vote.domain.VoteStatus;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService implements ChatCommandUseCase, ChatQueryUseCase {

    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final VoteQueryUseCase voteQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final UserQueryUseCase userQueryUseCase;

    @Override
    public MessageResult sendMessage(SendMessageCommand command) {
        if (!voteParticipationQueryUseCase.isParticipant(command.voteId(), command.senderId())) {
            throw new ChatForbiddenException();
        }

        ChatMessage message = ChatMessage.of(command.voteId(), command.senderId(), command.content());
        if (message.isBlank()) {
            throw new InvalidMessageException();
        }

        ChatMessage saved = chatMessageRepository.save(message);
        User sender = userQueryUseCase.getUser(command.senderId());
        VoteOptionCode voteOptionCode =
                voteQueryUseCase.getSelectedOption(command.voteId(), command.senderId()).getCode();

        return new MessageResult(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                sender.getNickname(),
                sender.getImageColor(),
                voteOptionCode,
                true
        );
    }

    @Override
    public void markAsRead(MarkAsReadCommand command) {
        chatRoomUnreadRepository
                .findByIdUserIdAndIdVoteId(command.userId(), command.voteId())
                .ifPresentOrElse(
                        unread -> unread.updateLastRead(command.lastReadMessageId()),
                        () -> chatRoomUnreadRepository.save(
                                ChatRoomUnread.of(command.userId(), command.voteId(), command.lastReadMessageId())
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatListItemResult> getChatList(Long userId, VoteStatus status) {
        List<Long> voteIds = voteParticipationQueryUseCase.findAllVoteIdsByUserId(userId);
        List<Long> filteredVoteIds = voteQueryUseCase.findAllVoteIdsByStatus(voteIds, status);

        return filteredVoteIds.stream()
                .map(voteId -> {
                    VoteQueryUseCase.VoteChatSummary vote = voteQueryUseCase.getVoteChatSummary(voteId);
                    long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);

                    ChatMessage lastMsg = chatMessageRepository.findFirstByVoteIdOrderByIdDesc(voteId).orElse(null);
                    String lastMessage = lastMsg != null ? lastMsg.getContent() : null;

                    int unreadCount = calculateUnreadCount(userId, voteId);

                    return ChatListItemResult.of(
                            vote,
                            (int) participantCount,
                            lastMessage,
                            lastMsg != null ? lastMsg.getCreatedAt() : null,
                            unreadCount
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResult getChatRoom(Long voteId) {
        VoteQueryUseCase.VoteChatSummary vote = voteQueryUseCase.getVoteChatSummary(voteId);
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
        return ChatRoomResult.of(vote, (int) participantCount);
    }

    @Override
    @Transactional(readOnly = true)
    public GaugeResult getGauge(Long voteId) {
        VoteQueryUseCase.VoteRatio ratio = voteQueryUseCase.getRatio(voteId);
        return new GaugeResult(voteId, ratio.optionARatio(), ratio.optionBRatio(), ratio.participantCount());
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageResult getMessages(Long voteId, Long userId, Long cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<ChatMessage> messages;
        if (cursor == null) {
            messages = chatMessageRepository.findAllByVoteIdOrderByIdDesc(voteId, pageRequest);
        } else {
            messages = chatMessageRepository.findAllByVoteIdAndIdLessThanOrderByIdDesc(voteId, cursor, pageRequest);
        }

        // size+1 조회로 hasNext 판별 (별도 count 쿼리 없이)
        boolean hasNext = messages.size() > size;
        List<ChatMessage> pageMessages = hasNext ? messages.subList(0, size) : messages;
        Long nextCursor = hasNext ? pageMessages.get(pageMessages.size() - 1).getId() : null;

        List<MessageResult> results = pageMessages.stream()
                .map(msg -> {
                    User sender = userQueryUseCase.getUser(msg.getSenderId());
                    VoteOptionCode voteOptionCode =
                            voteQueryUseCase.getSelectedOption(voteId, msg.getSenderId()).getCode();

                    return new MessageResult(
                            msg.getId(),
                            msg.getContent(),
                            msg.getCreatedAt(),
                            sender.getNickname(),
                            sender.getImageColor(),
                            voteOptionCode,
                            msg.getSenderId().equals(userId)
                    );
                })
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }

    /**
     * 특정 투표 채팅방의 읽지 않은 메시지 수를 계산합니다.
     * - ChatRoomUnread 레코드가 없거나 lastReadMessageId가 null인 경우(한 번도 읽지 않은 경우)
     *   → 해당 방의 전체 메시지 수를 unread로 간주하여 반환
     * - 레코드가 있고 lastReadMessageId가 있으면 그 이후 메시지 수만 반환
     */
    private int calculateUnreadCount(Long userId, Long voteId) {
        return chatRoomUnreadRepository
                .findByIdUserIdAndIdVoteId(userId, voteId)
                .map(unread -> {
                    Long lastRead = unread.getLastReadMessageId();
                    if (lastRead == null) {
                        return (int) chatMessageRepository.countByVoteId(voteId);
                    }
                    return (int) chatMessageRepository.countByVoteIdAndIdGreaterThan(voteId, lastRead);
                })
                .orElseGet(() -> (int) chatMessageRepository.countByVoteId(voteId));
    }
}
