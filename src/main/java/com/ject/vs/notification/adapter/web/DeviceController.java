package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.RegisterPushTokenRequest;
import com.ject.vs.notification.port.in.PushTokenUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final PushTokenUseCase useCase;

    @PostMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid RegisterPushTokenRequest request) {
        if (userId == null) throw new UnauthorizedException();
        useCase.register(userId, request.token(), request.platform());
    }

    @DeleteMapping("/push-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterAll(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        useCase.unregisterAll(userId);
    }
}
