package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.UnreadPayload;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
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
    private final VoteQueryUseCase voteQueryUseCase;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final UserQueryUseCase userQueryUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        ChatMessage message = event.message();

        MessageResult messageResult = new MessageResult(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                resolveNickname(message.getSenderId()),
                null,
                resolveSelectedOptionCode(message.getVoteId(), message.getSenderId()),
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

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participantUserId),
                    "/topic/chat/" + message.getVoteId() + "/unread",
                    new UnreadPayload(message.getVoteId(), unreadCount)
            );
        }
    }

    private String resolveNickname(Long userId) {
        return userQueryUseCase.findById(userId)
                .map(User::getUserNameOrEmpty)
                .orElse(null);
    }

    private String resolveSelectedOptionCode(Long voteId, Long userId) {
        return voteQueryUseCase.getSelectedOptionCode(voteId, userId)
                .map(Enum::name)
                .orElse(null);
    }
}
