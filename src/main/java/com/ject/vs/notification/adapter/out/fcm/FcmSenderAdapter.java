package com.ject.vs.notification.adapter.out.fcm;

import com.google.firebase.messaging.*;
import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnBean(FirebaseMessaging.class)
@RequiredArgsConstructor
@Slf4j
public class FcmSenderAdapter implements PushSenderPort {

    private final FirebaseMessaging firebaseMessaging;

    @PostConstruct
    void logActivation() {
        log.info("Push sender: FCM enabled (FcmSenderAdapter active)");
    }

    @Override
    public List<String> sendMulticast(List<String> tokens, FcmPayload payload) {
        if (tokens.isEmpty()) return List.of();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build())
                .putAllData(payload.toDataMap())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.debug("FCM multicast successCount={} failureCount={}",
                    response.getSuccessCount(), response.getFailureCount());
            return collectExpiredTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast failed payload={}", payload.type(), e);
            return List.of();
        }
    }

    private List<String> collectExpiredTokens(List<String> tokens, BatchResponse response) {
        List<String> expired = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (!r.isSuccessful()) {
                MessagingErrorCode code = r.getException().getMessagingErrorCode();
                // UNREGISTERED만 "확실히 죽은 토큰"으로 보고 삭제한다.
                // INVALID_ARGUMENT는 페이로드 형식 문제로도 발생할 수 있어, 이를 만료로 간주하면
                // 멀쩡한 토큰까지 한꺼번에 지워져 이후 발송이 영구히 막힐 수 있다.
                if (code == MessagingErrorCode.UNREGISTERED) {
                    expired.add(tokens.get(i));
                } else {
                    log.warn("FCM send failed token={} code={}", tokens.get(i), code);
                }
            }
        }
        return expired;
    }
}
