package com.ject.vs.notification.port.out;

import com.ject.vs.notification.domain.NotificationType;

public record PushPayload(
        NotificationType type,
        String title,
        String body,
        Long voteId,
        String url) {

    public String toJson() {
        // 이스케이프 처리를 위해 따옴표 등 특수문자 제거
        String safeTitle = title == null ? "" : title.replace("\"", "\\\"");
        String safeBody = body == null ? "" : body.replace("\"", "\\\"");
        String safeUrl = url == null ? "" : url.replace("\"", "\\\"");
        return """
                {"type":"%s","title":"%s","body":"%s","data":{"voteId":%d,"url":"%s"}}
                """.formatted(type, safeTitle, safeBody, voteId, safeUrl).strip();
    }
}
