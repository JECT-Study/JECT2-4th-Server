package com.ject.vs.notification.adapter.web;

import com.ject.vs.analytics.AnalyticsEvent;
import com.ject.vs.analytics.AnalyticsEventLogger;
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
    private final AnalyticsEventLogger analytics;

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.")
    @GetMapping
    public NotificationListResponse getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) throw new UnauthorizedException();
        NotificationListResponse response = NotificationListResponse.from(queryUseCase.getList(userId, cursor, size));

        long unreadCount = response.notifications().stream()
                .filter(n -> !n.isRead())
                .count();

        analytics.log(AnalyticsEvent.of("notification_list_viewed")
                .put("notification_count", response.notifications().size())
                .put("has_next", response.hasNext())
                .put("next_cursor", response.nextCursor())
                .put("unread_count", unreadCount));

        return response;
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
        NotificationCommandUseCase.MarkAsReadResult result = commandUseCase.markAsRead(notificationId, userId);

        analytics.log(AnalyticsEvent.of("notification_opened")
                .put("notification_id", notificationId)
                .put("notification_type", result.type())
                .put("vote_id", result.voteId())
                .put("is_read", result.wasRead()));
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PostMapping("/read-all")
    public ReadAllResponse markAllAsRead(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        int updatedCount = commandUseCase.markAllAsRead(userId);
        return new ReadAllResponse(updatedCount);
    }
}
