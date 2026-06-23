package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.port.in.dto.ReplyInfo;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.port.in.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReplyInfoResolver {

    private static final String DELETED_NICKNAME = "알 수 없음";
    private static final String DELETED_CONTENT = "(삭제된 메시지)";

    private final ChatMessageRepository chatMessageRepository;
    private final UserQueryUseCase userQueryUseCase;

    public ReplyInfo resolve(Long parentMessageId) {
        if (parentMessageId == null) {
            return null;
        }

        return chatMessageRepository.findById(parentMessageId)
                .map(this::toReplyInfo)
                .orElseGet(() -> deletedReplyInfo(parentMessageId));
    }

    public Map<Long, ReplyInfo> resolveAll(List<Long> parentMessageIds) {
        if (parentMessageIds.isEmpty()) {
            return Map.of();
        }

        List<ChatMessage> parents = chatMessageRepository.findAllById(parentMessageIds);
        Map<Long, User> usersById = loadUsersById(parents);

        Map<Long, ReplyInfo> result = new HashMap<>();
        for (ChatMessage parent : parents) {
            User sender = usersById.get(parent.getSenderId());
            result.put(parent.getId(), new ReplyInfo(
                    parent.getId(),
                    sender.getNickname(),
                    parent.getContent()
            ));
        }

        for (Long parentMessageId : parentMessageIds) {
            result.putIfAbsent(parentMessageId, deletedReplyInfo(parentMessageId));
        }

        return result;
    }

    private ReplyInfo toReplyInfo(ChatMessage parent) {
        User sender = userQueryUseCase.getUser(parent.getSenderId());
        return new ReplyInfo(parent.getId(), sender.getNickname(), parent.getContent());
    }

    private Map<Long, User> loadUsersById(List<ChatMessage> messages) {
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();

        return userQueryUseCase.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ReplyInfo deletedReplyInfo(Long parentMessageId) {
        return new ReplyInfo(parentMessageId, DELETED_NICKNAME, DELETED_CONTENT);
    }
}