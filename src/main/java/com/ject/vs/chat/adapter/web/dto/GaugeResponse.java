package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.GaugeResult;

public record GaugeResponse(int optionARatio, int optionBRatio, int participantCount) {

    public static GaugeResponse from(GaugeResult result) {
        return new GaugeResponse(result.optionARatio(), result.optionBRatio(), result.participantCount());
    }
}
