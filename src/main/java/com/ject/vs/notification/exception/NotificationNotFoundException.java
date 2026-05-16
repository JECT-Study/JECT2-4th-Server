package com.ject.vs.notification.exception;

import com.ject.vs.common.exception.BusinessException;

public class NotificationNotFoundException extends BusinessException {
    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
