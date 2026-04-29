package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        ChatMessage message = event.message();

        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        String senderNickname = sender != null ? sender.getSub() : "unknown";

        MessageResult messageResult = new MessageResult(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                senderNickname,
                null,
                "A",
                false
        );

        messagingTemplate.convertAndSend("/topic/chat/" + message.getVoteId(), messageResult);

        broadcastUnreadCount(message);
    }

    private void broadcastUnreadCount(ChatMessage message) {
        for (Long participantUserId : voteParticipationQueryUseCase.findUserIdsByVoteId(message.getVoteId())) {
            long unreadCount = chatRoomUnreadRepository
                    .findByIdUserIdAndIdVoteId(participantUserId, message.getVoteId())
                    .map(unread -> chatMessageRepository.countByVoteIdAndIdGreaterThan(
                            message.getVoteId(), unread.getLastReadMessageId()))
                    .orElse(chatMessageRepository.countByVoteIdAndIdGreaterThan(message.getVoteId(), 0L));

            Map<String, Object> unreadPayload = new HashMap<>();
            unreadPayload.put("voteId", message.getVoteId());
            unreadPayload.put("unreadCount", unreadCount);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participantUserId),
                    "/topic/chat/" + message.getVoteId() + "/unread",
                    unreadPayload
            );
        }
    }
}
