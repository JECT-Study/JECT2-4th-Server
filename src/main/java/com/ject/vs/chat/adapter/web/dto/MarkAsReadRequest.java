package com.ject.vs.chat.adapter.web.dto;

import jakarta.validation.constraints.NotNull;

public record MarkAsReadRequest(@NotNull Long lastReadMessageId) {}
