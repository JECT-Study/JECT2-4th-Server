package com.ject.vs.common.domain;

import java.time.LocalDateTime;

public interface TimeTrackable {
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
