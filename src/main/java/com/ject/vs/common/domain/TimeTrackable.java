package com.ject.vs.common.domain;

import java.time.Instant;

public interface TimeTrackable {
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
