package com.ject.vs.notification.adapter.web.dto;

import com.ject.vs.notification.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPushTokenRequest(
        @NotBlank String token,
        @NotNull Platform platform) {
}
