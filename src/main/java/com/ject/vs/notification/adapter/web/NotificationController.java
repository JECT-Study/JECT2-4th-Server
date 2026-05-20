package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.NotificationListResponse;
import com.ject.vs.notification.adapter.web.dto.ReadAllResponse;
import com.ject.vs.notification.adapter.web.dto.UnreadCountResponse;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationQueryUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "알림 관련 API (회원 전용)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryUseCase queryUseCase;
    private final NotificationCommandUseCase commandUseCase;

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.")
    @GetMapping
    public NotificationListResponse getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationListResponse.from(queryUseCase.getList(userId, cursor, size));
    }

    @Operation(summary = "읽지 않은 알림 수 조회", description = "읽지 않은 알림의 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        return new UnreadCountResponse(queryUseCase.getUnreadCount(userId));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        commandUseCase.markAsRead(notificationId, userId);
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PostMapping("/read-all")
    public ReadAllResponse markAllAsRead(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        int updatedCount = commandUseCase.markAllAsRead(userId);
        return new ReadAllResponse(updatedCount);
    }
}
