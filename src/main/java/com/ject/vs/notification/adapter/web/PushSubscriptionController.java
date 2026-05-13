package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.PushSubscriptionResponse;
import com.ject.vs.notification.adapter.web.dto.RegisterPushSubscriptionRequest;
import com.ject.vs.notification.port.in.PushSubscriptionUseCase;
import com.ject.vs.vote.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/push-subscription")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionUseCase useCase;

    @PostMapping
    public PushSubscriptionResponse register(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestBody @Valid RegisterPushSubscriptionRequest request) {
        if (userId == null) throw new UnauthorizedException();
        Long subscriptionId = useCase.register(
                userId, request.endpoint(), request.p256dhKey(),
                request.authKey(), userAgent);
        return new PushSubscriptionResponse(subscriptionId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        useCase.unregisterAll(userId);
    }
}
