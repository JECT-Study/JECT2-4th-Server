package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.port.in.ChatQueryUseCase;
import com.ject.vs.chat.port.in.dto.ChatListItemResult;
import com.ject.vs.chat.port.in.dto.ChatRoomResult;
import com.ject.vs.chat.port.in.dto.GaugeResult;
import com.ject.vs.chat.port.in.dto.MessagePageResult;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
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
@Transactional(readOnly = true)
public class ChatQueryService implements ChatQueryUseCase {

    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final VoteQueryUseCase voteQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final UserRepository userRepository;

    @Override
    public List<ChatListItemResult> getChatList(Long userId, VoteStatus status) {
        List<Long> voteIds = voteParticipationQueryUseCase.findVoteIdsByUserId(userId);
        List<Long> filteredVoteIds = voteQueryUseCase.filterVoteIdsByStatus(voteIds, status);

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
    public ChatRoomResult getChatRoom(Long voteId) {
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
        return ChatRoomResult.of(voteId, (int) participantCount);
    }

    @Override
    public GaugeResult getGauge(Long voteId) {
        // TODO: Vote 도메인 연동 후 실제 득표율로 교체
        long participantCount = voteParticipationQueryUseCase.countParticipantsByVoteId(voteId);
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
                            null,   // TODO: Vote 도메인 연동 후 senderVoteOption 채워야 함
                            null,   // TODO: Vote 도메인 연동 후 senderProfileIconUrl 채워야 함
                            msg.getSenderId().equals(userId)
                    );
                })
                .toList();

        return new MessagePageResult(results, nextCursor, hasNext);
    }
}
