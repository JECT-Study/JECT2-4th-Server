package com.ject.vs.chat.adapter.event;

import com.ject.vs.chat.domain.*;
import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageReactionRepository;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.domain.MessageType;
import com.ject.vs.chat.domain.event.ChatMessageSentEvent;
import com.ject.vs.chat.port.in.dto.MessageResult;
import com.ject.vs.chat.port.in.dto.ReplyInfo;
import com.ject.vs.chat.port.in.dto.UnreadPayload;

import java.util.Map;
import com.ject.vs.user.domain.ImageColor;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import com.ject.vs.vote.domain.VoteOptionCode;
import com.ject.vs.vote.port.in.VoteParticipationQueryUseCase;
import com.ject.vs.vote.port.in.VoteQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.val;
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
    private final ChatMessageReactionRepository chatMessageReactionRepository;
    private final UserQueryUseCase userQueryUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageSentEvent event) {
        ChatMessage message = event.message();
        MessageType mt = message.getMessageType();
        Long sid = message.getSenderId();

        String nick;
        ImageColor col;
        VoteOptionCode voteOptionCode = null;
        if (sid == null || sid == 0L || mt == MessageType.SYSTEM) {
            nick = "시스템";
            col = null;
        } else {
            User sender = userQueryUseCase.getUser(sid);
            nick = sender.getNickname();
            col = sender.getImageColor();
            voteOptionCode = voteQueryUseCase.findSelectedOptionCode(message.getVoteId(), sid).orElse(null);
        }

        ReplyInfo replyInfo = buildReplyInfoForBroadcast(message.getParentMessageId());
        MessageResult messageResult = new MessageResult(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                nick,
                col,
                voteOptionCode,
                false,
                false,
                mt,
                replyInfo,
                Map.of(),
                null
        );

        messagingTemplate.convertAndSend("/topic/chat/" + message.getVoteId(), messageResult);

        broadcastUnreadCount(message);
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

    private ReplyInfo buildReplyInfoForBroadcast(Long parentMessageId) {
        if (parentMessageId == null) return null;

        return chatMessageRepository.findById(parentMessageId)
                .map(parent -> {
                    String preview = parent.getContent();  // 프론트에서 말줄임 처리
                    String nick = "시스템";
                    Long sid = parent.getSenderId();
                    if (sid != null && sid != 0L) {
                        try {
                            User u = userQueryUseCase.getUser(sid);
                            nick = u.getNickname();
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
}
