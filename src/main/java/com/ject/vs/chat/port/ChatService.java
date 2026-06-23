package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.*;
import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageReaction;
import com.ject.vs.chat.domain.ChatMessageReactionRepository;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.MessageType;
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

    @Override
    public MessageResult sendMessage(SendMessageCommand command) {
        if (!voteParticipationQueryUseCase.isParticipant(command.voteId(), command.senderId())) {
            throw new ChatForbiddenException();
        }

        Long parentId = command.replyToMessageId();
        if (parentId != null) {
            // 답글 대상 메시지가 동일 투표에 속하는지 검증
            ChatMessage parent = chatMessageRepository.findById(parentId)
                    .orElseThrow(() -> new InvalidMessageException());
            if (!parent.getVoteId().equals(command.voteId())) {
                throw new InvalidMessageException();
            }
        }

        ChatMessage message = ChatMessage.of(command.voteId(), command.senderId(), command.content(), parentId);
        if (message.isBlank()) {
            throw new InvalidMessageException();
        }

        // 행동 로그(is_first_message)용: 저장 전 기존 메시지 존재 여부로 첫 메시지인지 판단
        boolean isFirstMessage = chatMessageRepository.countByVoteId(command.voteId()) == 0;

        ChatMessage saved = chatMessageRepository.save(message);
        User sender = userQueryUseCase.getUser(command.senderId());
        VoteOptionCode voteOptionCode =
                voteQueryUseCase.findSelectedOptionCode(command.voteId(), command.senderId()).orElse(null);

        ReplyInfo replyInfo = buildReplyInfo(saved.getParentMessageId());

        return new MessageResult(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                sender.getNickname(),
                sender.getImageColor(),
                voteOptionCode,
                true,
                isFirstMessage,
                saved.getMessageType(),
                replyInfo,
                Map.of(),           // 신규 메시지는 반응 없음
                null
        );
    }

    private ReplyInfo buildReplyInfo(Long parentMessageId) {
        if (parentMessageId == null) return null;

        return chatMessageRepository.findById(parentMessageId)
                .map(parent -> {
                    String preview = parent.getContent();  // 프론트에서 말줄임 처리
                    String nick = "시스템";
                    if (parent.getSenderId() != null && parent.getSenderId() != 0L) {
                        try {
                            User pUser = userQueryUseCase.getUser(parent.getSenderId());
                            nick = pUser.getNickname();
                        } catch (Exception ignored) {}
                    }
                    return new ReplyInfo(parent.getId(), nick, preview);
                })
                .orElseGet(() -> new ReplyInfo(
                        parentMessageId,
                        "알 수 없음",
                        "(삭제된 메시지)"
                ));
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
        Map<Long, Map<ChatReactionType, Long>> reactionCountsMap = loadReactionCounts(messageIds);

        // 본인 반응
        Map<Long, ChatReactionType> myReactionMap = loadMyReactions(messageIds, userId);

        // parent preview 미리 로드 (간단히 findAllById)
        Map<Long, ReplyInfo> parentInfoMap = loadParentInfos(parentIds);

        List<MessageResult> results = pageMessages.stream()
                .map(msg -> {
                    MessageType mt = msg.getMessageType();
                    VoteOptionCode voteOptionCode = null;
                    String nick;
                    ImageColor col;
                    Long sid = msg.getSenderId();
                    if (sid == null || sid == 0L || mt == MessageType.SYSTEM) {
                        nick = "시스템";
                        col = null;
                    } else {
                        User sender = userQueryUseCase.getUser(sid);
                        nick = sender.getNickname();
                        col = sender.getImageColor();
                        voteOptionCode = voteQueryUseCase.findSelectedOptionCode(voteId, sid).orElse(null);
                    }

                    ReplyInfo replyInfo = msg.getParentMessageId() != null
                            ? parentInfoMap.get(msg.getParentMessageId())
                            : null;

                    Map<ChatReactionType, Long> counts = reactionCountsMap.getOrDefault(msg.getId(), Map.of());
                    ChatReactionType myReact = myReactionMap.get(msg.getId());

                    return new MessageResult(
                            msg.getId(),
                            msg.getContent(),
                            msg.getCreatedAt(),
                            nick,
                            col,
                            voteOptionCode,
                            sid != null && sid.equals(userId),
                            false,
                            mt,
                            replyInfo,
                            counts,
                            myReact
                    );
                })
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }

    private Map<Long, Map<ChatReactionType, Long>> loadReactionCounts(List<Long> messageIds) {
        if (messageIds.isEmpty()) return Map.of();
        Map<Long, Map<ChatReactionType, Long>> result = new java.util.HashMap<>();
        for (ReactionCount rc : chatMessageReactionRepository.countReactionsByMessageIds(messageIds)) {
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

    private Map<Long, ReplyInfo> loadParentInfos(List<Long> parentIds) {
        if (parentIds.isEmpty()) return Map.of();
        Map<Long, ReplyInfo> map = new java.util.HashMap<>();
        List<ChatMessage> parents = chatMessageRepository.findAllById(parentIds);
        for (ChatMessage p : parents) {
            String preview = p.getContent();  // 프론트에서 말줄임 처리
            String nick = "시스템";
            Long sid = p.getSenderId();
            if (sid != null && sid != 0L) {
                try {
                    User u = userQueryUseCase.getUser(sid);
                    if (u.getNickname() != null) nick = u.getNickname();
                } catch (Exception ignored) {}
            }
            map.put(p.getId(), new ReplyInfo(p.getId(), nick, preview));
        }

        // 부모를 찾지 못한 경우 (삭제된 메시지)도 id는 유지해서 반환
        for (Long pid : parentIds) {
            if (!map.containsKey(pid)) {
                map.put(pid, new ReplyInfo(pid, "알 수 없음", "(삭제된 메시지)"));
            }
        }
        return map;
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
    public void sendSystemMessage(Long voteId, String content) {
        ChatMessage sys = ChatMessage.ofSystem(voteId, content);
        chatMessageRepository.save(sys);
        // event listener will broadcast
    }

    @Override
    public ReactionResult reactToMessage(Long voteId, Long userId, Long messageId, ChatReactionType emoji) {
        if (!voteParticipationQueryUseCase.isParticipant(voteId, userId)) {
            throw new ChatForbiddenException();
        }

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidMessageException());

        if (!message.getVoteId().equals(voteId)) {
            throw new InvalidMessageException();
        }

        // 자신의 메시지에는 반응 불가
        if (message.getSenderId() != null && message.getSenderId().equals(userId)) {
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
        Map<ChatReactionType, Long> counts = loadSingleMessageReactionCounts(messageId);
        ChatReactionType myReaction = chatMessageReactionRepository
                .findByMessageIdAndUserId(messageId, userId)
                .map(ChatMessageReaction::getEmoji)
                .orElse(null);

        return new ReactionResult(messageId, counts, myReaction);
    }

    private Map<ChatReactionType, Long> loadSingleMessageReactionCounts(Long messageId) {
        Map<ChatReactionType, Long> counts = new java.util.HashMap<>();
        for (ReactionCount rc : chatMessageReactionRepository.countReactionsByMessageIds(List.of(messageId))) {
            counts.put(rc.emoji(), rc.count());
        }
        return counts;
    }
}
