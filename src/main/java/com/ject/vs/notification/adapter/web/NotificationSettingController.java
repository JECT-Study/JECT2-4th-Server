package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.NotificationSettingResponse;
import com.ject.vs.notification.adapter.web.dto.UpdateNotificationSettingRequest;
import com.ject.vs.notification.port.in.NotificationSettingUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/notification-setting")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingUseCase useCase;

    @GetMapping
    public NotificationSettingResponse get(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationSettingResponse.from(useCase.get(userId));
    }

    @PutMapping
    public NotificationSettingResponse update(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateNotificationSettingRequest request) {
        if (userId == null) throw new UnauthorizedException();
        return NotificationSettingResponse.from(
                useCase.updatePushEnabled(userId, request.pushEnabled()));
    }
}
