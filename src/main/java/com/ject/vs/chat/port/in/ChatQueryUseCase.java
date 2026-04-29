package com.ject.vs.chat.port.in;

import com.ject.vs.chat.port.in.dto.ChatListItemResult;
import com.ject.vs.chat.port.in.dto.ChatRoomResult;
import com.ject.vs.chat.port.in.dto.GaugeResult;
import com.ject.vs.chat.port.in.dto.MessagePageResult;
import com.ject.vs.chat.port.in.dto.VoteStatus;

import java.util.List;

public interface ChatQueryUseCase {
    List<ChatListItemResult> getChatList(Long userId, VoteStatus status);
    ChatRoomResult getChatRoom(Long voteId);
    GaugeResult getGauge(Long voteId);
    MessagePageResult getMessages(Long voteId, Long userId, Long cursor, int size);
}
