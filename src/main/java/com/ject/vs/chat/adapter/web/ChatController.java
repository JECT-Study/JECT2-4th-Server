package com.ject.vs.chat.adapter.web;

import com.ject.vs.chat.adapter.web.dto.*;
import com.ject.vs.chat.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatQueryUseCase chatQueryUseCase;
    private final ChatCommandUseCase chatCommandUseCase;

    @GetMapping
    public ChatListResponse getChatList(@AuthenticationPrincipal Long userId,
                                        @RequestParam VoteStatus status) {
        return new ChatListResponse(
                chatQueryUseCase.getChatList(userId, status).stream()
                        .map(ChatListItemResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{voteId}")
    public ChatRoomResponse getChatRoom(@PathVariable Long voteId) {
        return ChatRoomResponse.from(chatQueryUseCase.getChatRoom(voteId));
    }

    @GetMapping("/{voteId}/gauge")
    public GaugeResponse getGauge(@PathVariable Long voteId) {
        return GaugeResponse.from(chatQueryUseCase.getGauge(voteId));
    }

    @GetMapping("/{voteId}/messages")
    public MessagePageResponse getMessages(@PathVariable Long voteId,
                                           @AuthenticationPrincipal Long userId,
                                           @RequestParam(required = false) Long cursor,
                                           @RequestParam(defaultValue = "30") int size) {
        return MessagePageResponse.from(chatQueryUseCase.getMessages(voteId, userId, cursor, size));
    }

    @PostMapping("/{voteId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable Long voteId,
                                       @AuthenticationPrincipal Long userId,
                                       @RequestBody @Valid SendMessageRequest request) {
        SendMessageCommand command = new SendMessageCommand(voteId, userId, request.content());
        return MessageResponse.from(chatCommandUseCase.sendMessage(command));
    }

    @PostMapping("/{voteId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long voteId,
                           @AuthenticationPrincipal Long userId,
                           @RequestBody @Valid MarkAsReadRequest request) {
        chatCommandUseCase.markAsRead(new MarkAsReadCommand(voteId, userId, request.lastReadMessageId()));
    }
}
