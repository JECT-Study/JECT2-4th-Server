package com.ject.vs.chat.port.in.dto;

public record GaugeResult(Long voteId, int optionARatio, int optionBRatio, int participantCount) {}
