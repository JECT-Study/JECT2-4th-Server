package com.ject.vs.notification.adapter.web;

import com.ject.vs.notification.adapter.web.dto.PromptStatusResponse;
import com.ject.vs.notification.port.in.NotificationPromptUseCase;
import com.ject.vs.notification.port.in.NotificationPromptUseCase.PromptStatusResult;
import com.ject.vs.vote.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림 권한 프롬프트", description = "알림 권한 요청 프롬프트 관련 API (회원 전용)")
@RestController
@RequestMapping("/api/me/notification-prompt")
@RequiredArgsConstructor
public class NotificationPromptController {

    private final NotificationPromptUseCase useCase;

    @Operation(summary = "프롬프트 표시 여부 조회", description = "알림 권한 요청 프롬프트를 표시해야 하는지 여부를 조회합니다.")
    @GetMapping("/status")
    public PromptStatusResponse getStatus(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        PromptStatusResult result = useCase.getStatus(userId);
        return new PromptStatusResponse(result.shouldShow(), result.totalParticipationCount());
    }

    @Operation(summary = "프롬프트 거절 기록", description = "사용자가 알림 권한 프롬프트를 거절했음을 기록합니다.")
    @PostMapping("/dismissed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordDismissed(@AuthenticationPrincipal Long userId) {
        if (userId == null) throw new UnauthorizedException();
        useCase.recordDismissed(userId);
    }
}
