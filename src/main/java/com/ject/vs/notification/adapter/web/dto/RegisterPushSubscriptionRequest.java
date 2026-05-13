package com.ject.vs.notification.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterPushSubscriptionRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dhKey,
        @NotBlank String authKey) {
}
