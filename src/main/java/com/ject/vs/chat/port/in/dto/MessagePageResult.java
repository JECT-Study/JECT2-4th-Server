package com.ject.vs.chat.port.in.dto;

import java.util.List;

public record MessagePageResult(List<MessageResult> messages, Long nextCursor, boolean hasNext) {}
