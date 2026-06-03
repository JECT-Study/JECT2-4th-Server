package com.ject.vs.vote.adapter.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record ImmersiveNextRequest(
        List<Long> excludeIds,

        @Min(1)
        @Max(50)
        Integer size
) {
    public ImmersiveNextRequest {
        if (size == null) {
            size = 10;
        }
    }
}
