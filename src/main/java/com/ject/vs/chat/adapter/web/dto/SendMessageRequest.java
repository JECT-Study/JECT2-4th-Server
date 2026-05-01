package com.ject.vs.chat.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(@NotBlank String content) {}
