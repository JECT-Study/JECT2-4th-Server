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
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import com.ject.vs.vote.port.in.dto.VoteStatus;
import lombok.RequiredArgsConstructor;
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

        return new MessageResult(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                "User#" + command.senderId(), // TODO: User.nickname 추가 후 교체
                null,
                null,   // TODO: Vote 도메인 연동 후 senderVoteOption 채워야 함
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
                    long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);

                    ChatMessage lastMsg = chatMessageRepository.findFirstByVoteIdOrderByIdDesc(voteId).orElse(null);
                    String lastMessage = lastMsg != null ? lastMsg.getContent() : null;

                    int unreadCount = chatRoomUnreadRepository
                            .findByIdUserIdAndIdVoteId(userId, voteId)
                            .map(unread -> (int) chatMessageRepository.countByVoteIdAndIdGreaterThan(voteId, unread.getLastReadMessageId()))
                            .orElse(0);

                    return ChatListItemResult.of(
                            voteId,
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
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
        return ChatRoomResult.of(voteId, (int) participantCount);
    }

    @Override
    @Transactional(readOnly = true)
    public GaugeResult getGauge(Long voteId) {
        // TODO: Vote 도메인 연동 후 실제 득표율로 교체
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
        return new GaugeResult(50, 50, (int) participantCount);
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
                .map(msg -> new MessageResult(
                        msg.getId(),
                        msg.getContent(),
                        msg.getCreatedAt(),
                        "User#" + msg.getSenderId(), // TODO: User.nickname 추가 후 교체
                        null,
                        null,   // TODO: Vote 도메인 연동 후 senderVoteOption 채워야 함
                        msg.getSenderId().equals(userId)
                ))
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }
}
