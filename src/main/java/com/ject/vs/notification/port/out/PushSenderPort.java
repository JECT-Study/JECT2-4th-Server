package com.ject.vs.notification.port.out;

import java.util.List;

public interface PushSenderPort {

    /**
     * 멀티캐스트 발송. 만료된 토큰(UNREGISTERED / INVALID_ARGUMENT) 목록 반환.
     */
    List<String> sendMulticast(List<String> tokens, FcmPayload payload);
}
