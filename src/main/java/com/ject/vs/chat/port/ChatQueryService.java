package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.port.in.*;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.domain.VoteParticipation;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService implements ChatQueryUseCase {

    private final VoteParticipationRepository voteParticipationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChatListItemResult> getChatList(Long userId, VoteStatus status) {
        List<VoteParticipation> participations = voteParticipationRepository.findByUserId(userId);

        return participations.stream()
                .map(participation -> {
                    Long voteId = participation.getVoteId();
                    long participantCount = voteParticipationRepository.countByVoteId(voteId);

                    ChatMessage lastMsg = chatMessageRepository.findFirstByVoteIdOrderByIdDesc(voteId).orElse(null);
                    String lastMessage = lastMsg != null ? lastMsg.getContent() : null;
                    LocalDateTime lastMessageAt = lastMsg != null ? lastMsg.getCreatedAt() : null;

                    int unreadCount = chatRoomUnreadRepository
                            .findByIdUserIdAndIdVoteId(userId, voteId)
                            .map(unread -> (int) chatMessageRepository.countByVoteIdAndIdGreaterThan(voteId, unread.getLastReadMessageId()))
                            .orElse(0);

                    return new ChatListItemResult(
                            voteId,
                            "투표 #" + voteId,
                            null,
                            "옵션 A",
                            "옵션 B",
                            (int) participantCount,
                            lastMessage,
                            lastMessageAt,
                            LocalDateTime.now().plusDays(1),
                            unreadCount
                    );
                })
                .toList();
    }

    @Override
    public ChatRoomResult getChatRoom(Long voteId) {
        long participantCount = voteParticipationRepository.countByVoteId(voteId);
        return new ChatRoomResult(
                voteId,
                "투표 #" + voteId,
                VoteStatus.ONGOING,
                (int) participantCount,
                "옵션 A",
                "옵션 B",
                LocalDateTime.now().plusDays(1)
        );
    }

    @Override
    public GaugeResult getGauge(Long voteId) {
        long participantCount = voteParticipationRepository.countByVoteId(voteId);
        return new GaugeResult(50, 50, (int) participantCount);
    }

    @Override
    public MessagePageResult getMessages(Long voteId, Long userId, Long cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<ChatMessage> messages;
        if (cursor == null) {
            messages = chatMessageRepository.findByVoteIdOrderByIdDesc(voteId, pageRequest);
        } else {
            messages = chatMessageRepository.findByVoteIdAndIdLessThanOrderByIdDesc(voteId, cursor, pageRequest);
        }

        boolean hasNext = messages.size() > size;
        List<ChatMessage> pageMessages = hasNext ? messages.subList(0, size) : messages;

        Long nextCursor = hasNext ? pageMessages.get(pageMessages.size() - 1).getId() : null;

        List<MessageResult> results = pageMessages.stream()
                .map(msg -> {
                    User sender = userRepository.findById(msg.getSenderId()).orElse(null);
                    String senderNickname = sender != null ? sender.getSub() : "unknown";
                    return new MessageResult(
                            msg.getId(),
                            msg.getContent(),
                            msg.getCreatedAt(),
                            senderNickname,
                            null,
                            "A",
                            msg.getSenderId().equals(userId)
                    );
                })
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }
}
