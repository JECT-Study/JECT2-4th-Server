package com.ject.vs.notification.exception;

import com.ject.vs.common.exception.BusinessException;

public class NotificationAccessDeniedException extends BusinessException {
    public NotificationAccessDeniedException() {
        super(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
    }
}
