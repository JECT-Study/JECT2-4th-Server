package com.ject.vs.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtils {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeUtils() {}

    public static OffsetDateTime toKstOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(KST).toOffsetDateTime();
    }
}
