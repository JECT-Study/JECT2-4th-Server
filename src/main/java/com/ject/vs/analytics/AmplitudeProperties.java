package com.ject.vs.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Amplitude HTTP V2 API 전송 설정.
 *
 * <p>서버 사이드에서 Amplitude로 이벤트를 보내려면 Amplitude 프로젝트의
 * <b>API Key</b>(Settings → Projects → 해당 프로젝트의 API Key)만 있으면 된다.
 * GA4와 달리 별도 secret은 필요 없다.
 *
 * <p>미설정(또는 {@code enabled=false})이면 {@link AmplitudeClient}가 no-op이 되어
 * 로컬·테스트 환경에서 외부 호출이 발생하지 않는다. (GA4 토글과 동일한 철학)
 */
@ConfigurationProperties(prefix = "amplitude")
public record AmplitudeProperties(
        boolean enabled,
        String apiKey,
        String endpoint
) {
    public AmplitudeProperties {
        if (endpoint == null || endpoint.isBlank()) {
            // EU 데이터 거주가 필요하면 https://api.eu.amplitude.com/2/httpapi 로 override
            endpoint = "https://api2.amplitude.com/2/httpapi";
        }
    }

    /** enabled이면서 API Key가 채워졌을 때만 실제 전송한다. */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
