package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.AdminPushRequest;
import com.ject.vs.notification.port.AdminPushService;
import com.ject.vs.vote.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "어드민 - 푸시 알림", description = "QA/테스트용 푸시 알림 강제 발송 API (어드민 전용)")
@RestController
@RequestMapping("/api/admin/push")
@RequiredArgsConstructor
public class AdminPushController {

    private final AdminPushService adminPushService;

    @Operation(
            summary = "테스트 푸시 알림 발송",
            description = "특정 사용자에게 테스트용 푸시 알림을 강제 발송합니다. 어드민 권한이 필요합니다."
    )
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public SendTestPushResponse sendTestPush(
            @AuthenticationPrincipal Long adminUserId,
            @Valid @RequestBody AdminPushRequest request
    ) {
        if (adminUserId == null) throw new UnauthorizedException();

        Long notificationId = adminPushService.sendTestPush(
                adminUserId,
                request.targetUserId(),
                request.title(),
                request.body(),
                request.voteId(),
                request.thumbnailUrl()
        );

        return new SendTestPushResponse(notificationId, "푸시 알림이 발송되었습니다.");
    }

    public record SendTestPushResponse(Long notificationId, String message) {}
}
