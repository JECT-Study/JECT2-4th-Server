package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.domain.ChatRoomUnread;
import com.ject.vs.chat.domain.ChatRoomUnreadRepository;
import com.ject.vs.chat.exception.ChatForbiddenException;
import com.ject.vs.chat.exception.InvalidMessageException;
import com.ject.vs.chat.port.in.ChatCommandUseCase;
import com.ject.vs.chat.port.in.MarkAsReadCommand;
import com.ject.vs.chat.port.in.MessageResult;
import com.ject.vs.chat.port.in.SendMessageCommand;
import com.ject.vs.domain.User;
import com.ject.vs.repository.UserRepository;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatCommandService implements ChatCommandUseCase {

    private final VoteParticipationRepository voteParticipationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUnreadRepository chatRoomUnreadRepository;
    private final UserRepository userRepository;

    @Override
    public MessageResult sendMessage(SendMessageCommand command) {
        if (!voteParticipationRepository.existsByVoteIdAndUserId(command.voteId(), command.senderId())) {
            throw new ChatForbiddenException();
        }

        ChatMessage message = ChatMessage.of(command.voteId(), command.senderId(), command.content());
        if (message.isBlank()) {
            throw new InvalidMessageException();
        }

        // save 시 DomainEventEntityListener(@PostPersist)가 ChatMessageSentEvent를 자동 발행
        ChatMessage saved = chatMessageRepository.save(message);

        User sender = userRepository.findById(command.senderId()).orElse(null);
        String senderNickname = sender != null ? sender.getSub() : "unknown";

        return new MessageResult(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                senderNickname,
                null,
                "A",
                true
        );
    }

    @Override
    public void markAsRead(MarkAsReadCommand command) {
        chatRoomUnreadRepository
                .findByIdUserIdAndIdVoteId(command.userId(), command.voteId())
                .ifPresentOrElse(
                        unread -> {
                            unread.updateLastRead(command.lastReadMessageId());
                            chatRoomUnreadRepository.save(unread);
                        },
                        () -> chatRoomUnreadRepository.save(
                                ChatRoomUnread.of(command.userId(), command.voteId(), command.lastReadMessageId())
                        )
                );
    }
}
