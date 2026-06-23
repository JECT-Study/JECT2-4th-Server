package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.*;
import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageReaction;
import com.ject.vs.chat.domain.ChatMessageReactionRepository;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatReactionUpdatedEvent;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.ChatMessageNotFoundException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService implements ChatCommandUseCase, ChatQueryUseCase {

    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final VoteQueryUseCase voteQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final ChatMessageReactionRepository chatMessageReactionRepository;
    private final UserQueryUseCase userQueryUseCase;
    private final ReplyInfoResolver replyInfoResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public MessageResult sendMessage(SendMessageCommand command) {
        if (!voteParticipationQueryUseCase.isParticipant(command.voteId(), command.senderId())) {
            throw new ChatForbiddenException();
        }

        User sender = userQueryUseCase.getUser(command.senderId());

        ChatMessage parentMessage = null;
        Long parentId = command.replyToMessageId();
        if (parentId != null) {
            parentMessage = chatMessageRepository.findById(parentId)
                    .orElseThrow(ChatMessageNotFoundException::new);
            if (!parentMessage.getVoteId().equals(command.voteId())) {
                throw new InvalidMessageException("답글 대상 메시지가 해당 채팅방에 속하지 않습니다.");
            }
        }

        ChatMessage message = ChatMessage.of(command.voteId(), sender, command.content(), parentMessage);
        if (message.isBlank()) {
            throw new InvalidMessageException("메시지 내용이 비어 있습니다.");
        }

        // 행동 로그(is_first_message)용: 저장 전 기존 메시지 존재 여부로 첫 메시지인지 판단
        boolean isFirstMessage = chatMessageRepository.countByVoteId(command.voteId()) == 0;

        ChatMessage saved = chatMessageRepository.save(message);
        VoteOptionCode voteOptionCode =
                voteQueryUseCase.findSelectedOptionCode(command.voteId(), command.senderId()).orElse(null);

        ReplyInfo replyInfo = replyInfoResolver.from(parentMessage);

        return new MessageResult(
                saved.getId(),
                saved.getSenderId(),
                saved.getContent(),
                saved.getCreatedAt(),
                sender.getNickname(),
                sender.getImageColor(),
                voteOptionCode,
                true,
                isFirstMessage,
                replyInfo,
                Map.of(),           // 신규 메시지는 반응 없음
                null
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
                .sorted(Comparator.comparing(
                        ChatListItemResult::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResult getChatRoom(Long voteId, Long userId) {
        VoteQueryUseCase.VoteChatSummary vote = voteQueryUseCase.getVoteChatSummary(voteId);
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
        VoteOptionCode myVoteOption = voteQueryUseCase.findSelectedOptionCode(voteId, userId).orElse(null);
        return ChatRoomResult.of(vote, (int) participantCount, myVoteOption);
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

        List<Long> messageIds = pageMessages.stream().map(ChatMessage::getId).toList();
        List<Long> parentIds = pageMessages.stream()
                .map(ChatMessage::getParentMessageId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        // 반응 집계 (messageId -> {emoji: count})
        Map<Long, Map<ChatReactionType, Long>> reactionsMap = loadReactions(messageIds);

        // 본인 반응
        Map<Long, ChatReactionType> myReactionMap = loadMyReactions(messageIds, userId);

        Map<Long, ReplyInfo> parentInfoMap = replyInfoResolver.resolveAll(parentIds);

        List<MessageResult> results = pageMessages.stream()
                .map(message -> {
                    User sender = message.getSender();
                    Long senderId = sender.getId();
                    String senderNickname = sender.getNickname();
                    ImageColor senderProfileIcon = sender.getImageColor();
                    VoteOptionCode voteOptionCode = voteQueryUseCase.findSelectedOptionCode(voteId, senderId).orElse(null);

                    ReplyInfo replyInfo = message.getParentMessageId() != null
                            ? parentInfoMap.get(message.getParentMessageId())
                            : null;

                    Map<ChatReactionType, Long> counts = reactionsMap.getOrDefault(message.getId(), Map.of());
                    ChatReactionType myReaction = myReactionMap.get(message.getId());

                    return new MessageResult(
                            message.getId(),
                            senderId,
                            message.getContent(),
                            message.getCreatedAt(),
                            senderNickname,
                            senderProfileIcon,
                            voteOptionCode,
                            senderId.equals(userId),
                            false,
                            replyInfo,
                            counts,
                            myReaction
                    );
                })
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }

    private Map<Long, Map<ChatReactionType, Long>> loadReactions(List<Long> messageIds) {
        if (messageIds.isEmpty()) return Map.of();
        Map<Long, Map<ChatReactionType, Long>> result = new java.util.HashMap<>();
        for (ReactionCount rc : chatMessageReactionRepository.countByMessageIds(messageIds)) {
            result.computeIfAbsent(rc.messageId(), k -> new java.util.HashMap<>())
                  .put(rc.emoji(), rc.count());
        }
        return result;
    }

    private Map<Long, ChatReactionType> loadMyReactions(List<Long> messageIds, Long userId) {
        if (messageIds.isEmpty() || userId == null) return Map.of();
        Map<Long, ChatReactionType> result = new java.util.HashMap<>();
        for (MyReaction mr : chatMessageReactionRepository.findMyReactionsByMessageIds(messageIds, userId)) {
            result.put(mr.messageId(), mr.emoji());
        }
        return result;
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

    @Override
    public ReactionResult reactToMessage(Long voteId, Long userId, Long messageId, ChatReactionType emoji) {
        if (!voteParticipationQueryUseCase.isParticipant(voteId, userId)) {
            throw new ChatForbiddenException();
        }

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(ChatMessageNotFoundException::new);

        if (!message.getVoteId().equals(voteId)) {
            throw new ChatMessageNotFoundException();
        }

        if (message.getSenderId().equals(userId)) {
            throw new ChatForbiddenException();
        }

        if (emoji == null) {
            // 취소
            chatMessageReactionRepository.deleteByMessageIdAndUserId(messageId, userId);
        } else {
            // 추가 또는 변경
            var existing = chatMessageReactionRepository.findByMessageIdAndUserId(messageId, userId);
            if (existing.isPresent()) {
                existing.get().changeEmoji(emoji);
            } else {
                ChatMessageReaction reaction = ChatMessageReaction.of(messageId, userId, emoji);
                chatMessageReactionRepository.save(reaction);
            }
        }

        // 최신 카운트 + 본인 상태 조회
        Map<ChatReactionType, Long> counts = loadSingleMessageReactions(messageId);
        ChatReactionType myReaction = chatMessageReactionRepository
                .findByMessageIdAndUserId(messageId, userId)
                .map(ChatMessageReaction::getEmoji)
                .orElse(null);

        eventPublisher.publishEvent(new ChatReactionUpdatedEvent(voteId, messageId, counts));

        return new ReactionResult(messageId, counts, myReaction);
    }

    private Map<ChatReactionType, Long> loadSingleMessageReactions(Long messageId) {
        Map<ChatReactionType, Long> counts = new java.util.HashMap<>();
        for (ReactionCount rc : chatMessageReactionRepository.countByMessageIds(List.of(messageId))) {
            counts.put(rc.emoji(), rc.count());
        }
        return counts;
    }
}
