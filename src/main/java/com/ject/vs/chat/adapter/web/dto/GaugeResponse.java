package com.ject.vs.chat.adapter.web.dto;

import com.ject.vs.chat.port.in.dto.GaugeResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 게이지 응답")
public record GaugeResponse(
        @Schema(description = "A 선택지 투표 비율. 0부터 100까지의 정수입니다.", example = "55", minimum = "0", maximum = "100")
        int optionARatio,

        @Schema(description = "B 선택지 투표 비율. 0부터 100까지의 정수입니다.", example = "45", minimum = "0", maximum = "100")
        int optionBRatio,

        @Schema(description = "투표 참여자 수", example = "42")
        int participantCount
) {

    public static GaugeResponse from(GaugeResult result) {
        return new GaugeResponse(result.optionARatio(), result.optionBRatio(), result.participantCount());
    }
}
