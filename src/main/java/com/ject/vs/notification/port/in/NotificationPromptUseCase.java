package com.ject.vs.notification.port.in;

public interface NotificationPromptUseCase {
    PromptStatusResult getStatus(Long userId);
    void recordDismissed(Long userId);

    record PromptStatusResult(boolean shouldShow, long totalParticipationCount) {
    }
}
