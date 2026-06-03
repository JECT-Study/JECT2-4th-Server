package com.ject.vs.user.adapter.web.dto;

import com.ject.vs.user.domain.ImageColor;
import jakarta.validation.constraints.NotNull;

public record UserProfileImageColorReq(@NotNull ImageColor imageColor) {
}
