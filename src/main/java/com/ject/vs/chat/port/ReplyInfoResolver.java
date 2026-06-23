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

    private final ChatMessageRepository chatMessageRepository;

    public ReplyInfo resolve(Long parentMessageId) {
        if (parentMessageId == null) {
            return null;
        }

        return chatMessageRepository.findByIdWithSender(parentMessageId)
                .map(this::toReplyInfo)
                .orElse(null);
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
        return result;
    }

    private ReplyInfo toReplyInfo(ChatMessage parent) {
        return new ReplyInfo(
                parent.getId(),
                parent.getSender().getNickname(),
                parent.getContent()
        );
    }
}