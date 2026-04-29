package com.ject.vs.chat.port.in;

import java.util.List;

public interface ChatQueryUseCase {
    List<ChatListItemResult> getChatList(Long userId, VoteStatus status);
    ChatRoomResult getChatRoom(Long voteId);
    GaugeResult getGauge(Long voteId);
    MessagePageResult getMessages(Long voteId, Long userId, Long cursor, int size);
}
