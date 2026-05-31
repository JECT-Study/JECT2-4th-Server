package com.ject.vs.notification.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "QA용 테스트 푸시 알림 발송 요청")
public record AdminPushRequest(
        @Schema(description = "푸시 알림을 받을 사용자 ID", example = "6")
        @NotNull(message = "userId는 필수입니다")
        Long targetUserId,

        @Schema(description = "알림 제목", example = "테스트 알림")
        @NotBlank(message = "title은 필수입니다")
        String title,

        @Schema(description = "알림 본문", example = "QA 테스트용 푸시 알림입니다.")
        @NotBlank(message = "body는 필수입니다")
        String body,

        @Schema(description = "연결할 투표 ID (선택)", example = "123")
        Long voteId,

        @Schema(description = "썸네일 URL (선택)", example = "https://example.com/image.jpg")
        String thumbnailUrl
) {
}
