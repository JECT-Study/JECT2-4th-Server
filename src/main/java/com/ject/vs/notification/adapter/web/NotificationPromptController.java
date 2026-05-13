package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.PromptStatusResponse;
import com.ject.vs.notification.port.in.NotificationPromptUseCase;
import com.ject.vs.notification.port.in.NotificationPromptUseCase.PromptStatusResult;
import com.ject.vs.vote.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/notification-prompt")
@RequiredArgsConstructor
public class NotificationPromptController {

    private final NotificationPromptUseCase useCase;

    @GetMapping("/status")
    public PromptStatusResponse getStatus(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        PromptStatusResult result = useCase.getStatus(userId);
        return new PromptStatusResponse(result.shouldShow(), result.totalParticipationCount());
    }

    @PostMapping("/dismissed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordDismissed(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        useCase.recordDismissed(userId);
    }
}
