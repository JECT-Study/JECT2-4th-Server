package com.ject.vs.notification.port.out;

import com.ject.vs.notification.domain.NotificationType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public record FcmPayload(
        NotificationType type,
        String title,
        String body,
        Long notificationId,
        Long voteId,
        String thumbnailUrl,
        Instant createdAt) {

    public Map<String, String> toDataMap() {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        data.put("notificationId", String.valueOf(notificationId));
        data.put("voteId", String.valueOf(voteId));
        data.put("thumbnailUrl", thumbnailUrl != null ? thumbnailUrl : "");
        data.put("createdAt", createdAt.atOffset(ZoneOffset.ofHours(9)).toString());
        return data;
    }
}
