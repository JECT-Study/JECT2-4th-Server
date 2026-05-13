package com.ject.vs.notification.exception;

import com.ject.vs.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다.", 404),
    NOTIFICATION_ACCESS_DENIED("알림에 접근 권한이 없습니다.", 403),
    PUSH_SUBSCRIPTION_INVALID("유효하지 않은 Push 구독 정보입니다.", 400);

    private final String message;
    private final Integer statusCode;

    @Override
    public String getCode() {
        return this.name();
    }
}
