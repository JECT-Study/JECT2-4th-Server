package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.domain.event.ChatReactionUpdatedEvent;
import com.ject.vs.chat.port.ReplyInfoResolver;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.ReactionUpdatedPayload;
import com.ject.vs.chat.port.in.dto.ReplyInfo;
import com.ject.vs.chat.port.in.dto.UnreadPayload;

import java.util.Map;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.vote.domain.VoteOptionCode;
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
    private final ReplyInfoResolver replyInfoResolver;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        ChatMessage message = event.message();
        User sender = message.getSender();
        Long senderId = sender.getId();
        String senderNickname = sender.getNickname();
        ImageColor senderProfileIcon = sender.getImageColor();
        VoteOptionCode voteOptionCode = voteQueryUseCase.findSelectedOptionCode(message.getVoteId(), senderId).orElse(null);

        ReplyInfo replyInfo = replyInfoResolver.from(message.getParentMessage());
        MessageResult messageResult = new MessageResult(
                message.getId(),
                senderId,
                message.getContent(),
                message.getCreatedAt(),
                senderNickname,
                senderProfileIcon,
                voteOptionCode,
                false,
                false,
                replyInfo,
                Map.of(),
                null
        );

        messagingTemplate.convertAndSend("/topic/chat/" + message.getVoteId(), messageResult);

        broadcastUnreadCount(message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReactionUpdated(ChatReactionUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/chat/" + event.voteId(),
                ReactionUpdatedPayload.of(event.messageId(), event.reactions())
        );
    }

    private void broadcastUnreadCount(ChatMessage message) {
        long totalCount = chatMessageRepository.countByVoteId(message.getVoteId());

        for (Long participantUserId : voteParticipationQueryUseCase.findAllUserIdsByVoteId(message.getVoteId())) {
            long unreadCount = chatRoomUnreadRepository
                    .findByIdUserIdAndIdVoteId(participantUserId, message.getVoteId())
                    .map(unread -> {
                        Long lastRead = unread.getLastReadMessageId();
                        if (lastRead == null) {
                            return totalCount;
                        }
                        return chatMessageRepository.countByVoteIdAndIdGreaterThan(
                                message.getVoteId(), lastRead);
                    })
                    .orElse(totalCount);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(participantUserId),
                    "/topic/chat/" + message.getVoteId() + "/unread",
                    new UnreadPayload(message.getVoteId(), unreadCount)
            );
        }
    }

}
