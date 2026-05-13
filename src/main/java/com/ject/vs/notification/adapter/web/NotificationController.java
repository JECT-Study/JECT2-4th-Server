package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.NotificationListResponse;
import com.ject.vs.notification.adapter.web.dto.UnreadCountResponse;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationQueryUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryUseCase queryUseCase;
    private final NotificationCommandUseCase commandUseCase;

    @GetMapping
    public NotificationListResponse getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationListResponse.from(queryUseCase.getList(userId, cursor, size));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        return new UnreadCountResponse(queryUseCase.getUnreadCount(userId));
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        commandUseCase.markAsRead(notificationId, userId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        commandUseCase.markAllAsRead(userId);
    }
}
