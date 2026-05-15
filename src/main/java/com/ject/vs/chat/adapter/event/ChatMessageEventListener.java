package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.UnreadPayload;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final VoteParticipationQueryUseCase voteParticipationQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        ChatMessage message = event.message();

        MessageResult messageResult = new MessageResult(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                "User#" + message.getSenderId(), // TODO: User.nickname 추가 후 교체
                null,
                null,   // TODO: Vote 도메인 연동 후 senderVoteOption 채워야 함
                false
        );

        messagingTemplate.convertAndSend("/topic/chat/" + message.getVoteId(), messageResult);

        broadcastUnreadCount(message);
    }

    private void broadcastUnreadCount(ChatMessage message) {
        long totalCount = chatMessageRepository.countByVoteId(message.getVoteId());

        for (Long participantUserId : voteParticipationQueryUseCase.findAllUserIdsByVoteId(message.getVoteId())) {
            long unreadCount = chatRoomUnreadRepository
                    .findByIdUserIdAndIdVoteId(participantUserId, message.getVoteId())
                    .map(unread -> chatMessageRepository.countByVoteIdAndIdGreaterThan(
                            message.getVoteId(), unread.getLastReadMessageId()))
                    .orElse(totalCount);

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getVoteId() + "/unread/" + participantUserId,
                    new UnreadPayload(message.getVoteId(), unreadCount)
            );
        }
    }
}
