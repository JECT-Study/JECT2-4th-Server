package com.ject.vs.chat.port;

import com.ject.vs.chat.domain.ChatMessage;
import com.ject.vs.chat.domain.ChatMessageRepository;
import com.ject.vs.chat.port.in.dto.ReplyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReplyInfoResolver {

    private static final String DELETED_NICKNAME = "알 수 없음";
    private static final String DELETED_CONTENT = "(삭제된 메시지)";

    private final ChatMessageRepository chatMessageRepository;

    public ReplyInfo resolve(Long parentMessageId) {
        if (parentMessageId == null) {
            return null;
        }

        return chatMessageRepository.findByIdWithSender(parentMessageId)
                .map(this::toReplyInfo)
                .orElseGet(() -> deletedReplyInfo(parentMessageId));
    }

    public ReplyInfo from(ChatMessage parentMessage) {
        if (parentMessage == null) {
            return null;
        }
        return toReplyInfo(parentMessage);
    }

    public Map<Long, ReplyInfo> resolveAll(List<Long> parentMessageIds) {
        if (parentMessageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ReplyInfo> result = new HashMap<>();
        for (ChatMessage parent : chatMessageRepository.findAllByIdWithSender(parentMessageIds)) {
            result.put(parent.getId(), toReplyInfo(parent));
        }

        for (Long parentMessageId : parentMessageIds) {
            result.putIfAbsent(parentMessageId, deletedReplyInfo(parentMessageId));
        }

        return result;
    }

    private ReplyInfo toReplyInfo(ChatMessage parent) {
        return new ReplyInfo(
                parent.getId(),
                parent.getSender().getNickname(),
                parent.getContent()
        );
    }

    private ReplyInfo deletedReplyInfo(Long parentMessageId) {
        return new ReplyInfo(parentMessageId, DELETED_NICKNAME, DELETED_CONTENT);
    }
}