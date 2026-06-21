package com.ject.vs.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GA4 Measurement Protocol 전송 설정.
 *
 * <p>서버 사이드에서 GA4로 이벤트를 보내려면 구글 애널리틱스 콘솔에서 발급한
 * <b>Measurement ID</b>(웹 데이터 스트림, {@code G-XXXXXXXX})와
 * <b>API secret</b>(Admin → Data Streams → Measurement Protocol API secrets)이 필요하다.
 * 구글 계정 아이디/비밀번호는 서버가 직접 쓰지 않는다(콘솔 로그인용일 뿐).
 *
 * <p>미설정(또는 {@code enabled=false})이면 {@link GoogleAnalyticsClient}가 no-op이 되어
 * 로컬·테스트 환경에서 외부 호출이 발생하지 않는다. (Gemini 토글과 동일한 철학)
 */
@ConfigurationProperties(prefix = "google.analytics")
public record GoogleAnalyticsProperties(
        boolean enabled,
        String measurementId,
        String apiSecret,
        String endpoint
) {
    public GoogleAnalyticsProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://www.google-analytics.com/mp/collect";
        }
    }

    /** enabled이면서 자격증명이 모두 채워졌을 때만 실제 전송한다. */
    public boolean isConfigured() {
        return enabled
                && measurementId != null && !measurementId.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
