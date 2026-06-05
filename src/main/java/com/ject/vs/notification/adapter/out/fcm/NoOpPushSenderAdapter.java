package com.ject.vs.notification.adapter.out.fcm;

import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("'${firebase.service-account-path:}' == ''")
@Slf4j
public class NoOpPushSenderAdapter implements PushSenderPort {

    @PostConstruct
    void logActivation() {
        // firebase.service-account-path가 비어 있어 FCM이 비활성화된 상태.
        // 운영에서 푸시가 전혀 안 갈 때 이 로그로 원인을 즉시 식별할 수 있도록 WARN으로 남긴다.
        log.warn("Push sender: FCM DISABLED (NoOpPushSenderAdapter active) — "
                + "firebase.service-account-path 미설정. 푸시 알림이 발송되지 않습니다.");
    }

    @Override
    public List<String> sendMulticast(List<String> tokens, FcmPayload payload) {
        log.debug("FCM is disabled; skipping push notification payload={}", payload.type());
        return List.of();
    }
}
