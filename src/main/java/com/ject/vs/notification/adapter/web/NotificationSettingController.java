package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.NotificationSettingResponse;
import com.ject.vs.notification.adapter.web.dto.UpdateNotificationSettingRequest;
import com.ject.vs.notification.port.in.NotificationSettingUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림 설정", description = "알림 설정 관련 API (회원 전용)")
@RestController
@RequestMapping("/api/me/notification-setting")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingUseCase useCase;

    @Operation(summary = "알림 설정 조회", description = "현재 알림 설정 상태를 조회합니다.")
    @GetMapping
    public NotificationSettingResponse get(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationSettingResponse.from(useCase.get(userId));
    }

    @Operation(summary = "알림 설정 변경", description = "푸시 알림 ON/OFF 설정을 변경합니다.")
    @PutMapping
    public NotificationSettingResponse update(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateNotificationSettingRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationSettingResponse.from(
                useCase.updatePushEnabled(userId, request.pushEnabled()));
    }
}
