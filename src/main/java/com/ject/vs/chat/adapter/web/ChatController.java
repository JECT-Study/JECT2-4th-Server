package com.ject.vs.chat.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
import com.ject.vs.chat.adapter.web.dto.*;
import com.ject.vs.chat.port.in.*;
import com.ject.vs.chat.port.in.dto.*;
import com.ject.vs.vote.domain.VoteStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController implements ChatDocs {

    private final ChatQueryUseCase chatQueryUseCase;
    private final ChatCommandUseCase chatCommandUseCase;
    private final AnalyticsEventLogger analytics;

    @GetMapping
    @Override
    public ChatListResponse getChatList(@AuthenticationPrincipal Long userId,
                                        @RequestParam VoteStatus status) {
        List<ChatListItemResponse> chats = chatQueryUseCase.getChatList(userId, status).stream()
                .map(ChatListItemResponse::from)
                .toList();

        // unread_total_count: 각 채팅방의 안 읽은 메시지 수를 서버에서 합산(SUM)
        long unreadTotalCount = chats.stream()
                .mapToLong(ChatListItemResponse::unreadCount)
                .sum();

        analytics.log(AnalyticsEvent.of("chat_list_viewed")
                .put("status", status)
                .put("chat_count", chats.size())
                .put("unread_total_count", unreadTotalCount));

        return new ChatListResponse(chats);
    }

    @GetMapping("/{voteId}")
    @Override
    public ChatRoomResponse getChatRoom(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long voteId) {
        ChatRoomResponse response = ChatRoomResponse.from(chatQueryUseCase.getChatRoom(voteId));

        analytics.log(AnalyticsEvent.of("chat_room_entered")
                .put("vote_id", response.voteId())
                .put("vote_status", response.status())
                .put("participant_count", response.participantCount())
                .put("end_at", response.endAt()));

        return response;
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
        MessagePageResponse response = MessagePageResponse.from(chatQueryUseCase.getMessages(voteId, userId, cursor, size));

        analytics.log(AnalyticsEvent.of("chat_messages_viewed")
                .put("vote_id", voteId)
                .put("message_count", response.messages().size())
                .put("next_cursor", response.nextCursor())
                .put("has_next", response.hasNext()));

        return response;
    }

    @PostMapping("/{voteId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public MessageResponse sendMessage(@PathVariable Long voteId,
                                       @AuthenticationPrincipal Long userId,
                                       @RequestBody @Valid SendMessageRequest request) {
        SendMessageCommand command = new SendMessageCommand(voteId, userId, request.content());
        MessageResult result = chatCommandUseCase.sendMessage(command);

        analytics.log(AnalyticsEvent.of("chat_message_sent")
                .put("vote_id", voteId)
                .put("message_id", result.messageId())
                .put("message_length", request.content() != null ? request.content().length() : 0)
                .put("sender_vote_option", result.senderVoteOption())
                .put("is_mine", result.isMine())
                .put("is_first_message", result.isFirstMessage()));

        return MessageResponse.from(result);
    }

    @PostMapping("/{voteId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void markAsRead(@PathVariable Long voteId,
                           @AuthenticationPrincipal Long userId,
                           @RequestBody @Valid MarkAsReadRequest request) {
        chatCommandUseCase.markAsRead(new MarkAsReadCommand(voteId, userId, request.lastReadMessageId()));

        analytics.log(AnalyticsEvent.of("chat_read_updated")
                .put("vote_id", voteId)
                .put("last_read_message_id", request.lastReadMessageId()));
    }
}
