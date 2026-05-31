package com.ject.vs.chat.adapter.web;

import com.ject.vs.chat.adapter.web.dto.*;
import com.ject.vs.chat.port.in.*;
import com.ject.vs.chat.port.in.dto.*;
import com.ject.vs.vote.domain.VoteStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController implements ChatDocs {

    private final ChatQueryUseCase chatQueryUseCase;
    private final ChatCommandUseCase chatCommandUseCase;

    @GetMapping
    @Override
    public ChatListResponse getChatList(@AuthenticationPrincipal Long userId,
                                        @RequestParam VoteStatus status) {
        return new ChatListResponse(
                chatQueryUseCase.getChatList(userId, status).stream()
                        .map(ChatListItemResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{voteId}")
    @Override
    public ChatRoomResponse getChatRoom(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long voteId) {
        return ChatRoomResponse.from(chatQueryUseCase.getChatRoom(voteId));
    }

    @GetMapping("/{voteId}/gauge")
    @Override
    public GaugeResponse getGauge(@PathVariable Long voteId) {
        return GaugeResponse.from(chatQueryUseCase.getGauge(voteId));
    }

    @GetMapping("/{voteId}/messages")
    @Override
    public MessagePageResponse getMessages(@PathVariable Long voteId,
                                           @AuthenticationPrincipal Long userId,
                                           @RequestParam(required = false) Long cursor,
                                           @RequestParam(defaultValue = "30") int size) {
        return MessagePageResponse.from(chatQueryUseCase.getMessages(voteId, userId, cursor, size));
    }

    @PostMapping("/{voteId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public MessageResponse sendMessage(@PathVariable Long voteId,
                                       @AuthenticationPrincipal Long userId,
                                       @RequestBody @Valid SendMessageRequest request) {
        SendMessageCommand command = new SendMessageCommand(voteId, userId, request.content());
        return MessageResponse.from(chatCommandUseCase.sendMessage(command));
    }

    @PostMapping("/{voteId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void markAsRead(@PathVariable Long voteId,
                           @AuthenticationPrincipal Long userId,
                           @RequestBody @Valid MarkAsReadRequest request) {
        chatCommandUseCase.markAsRead(new MarkAsReadCommand(voteId, userId, request.lastReadMessageId()));
    }
}
