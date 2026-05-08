package com.ject.vs.vote.adapter.web.dto;

import jakarta.validation.constraints.NotNull;

public record ParticipateRequest(@NotNull Long optionId) {
}
