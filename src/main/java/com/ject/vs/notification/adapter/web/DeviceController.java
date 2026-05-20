package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.RegisterPushTokenRequest;
import com.ject.vs.notification.port.in.PushTokenUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "푸시 알림", description = "푸시 토큰 등록/해제 API (회원 전용)")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final PushTokenUseCase useCase;

    @Operation(summary = "푸시 토큰 등록", description = "FCM/APNs 디바이스 토큰을 등록합니다. 투표 종료 시 푸시 알림 발송에 사용됩니다.")
    @PostMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RegisterPushTokenRequest request) {
        if (userId == null) throw new UnauthorizedException();
        useCase.register(userId, request.token(), request.platform());
    }

    @Operation(summary = "푸시 토큰 해제", description = "등록된 모든 푸시 토큰을 해제합니다.")
    @DeleteMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterAll(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        useCase.unregisterAll(userId);
    }
}
