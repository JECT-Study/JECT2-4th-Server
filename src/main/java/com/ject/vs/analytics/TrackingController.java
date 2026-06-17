package com.ject.vs.analytics;

import com.ject.vs.config.UtmCookie;
import com.ject.vs.user.domain.UtmAttribution;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유입 추적 엔드포인트.
 *
 * <p>UTM 파라미터는 프론트 SPA URL(예: {@code /vote/123?utm_source=instagram})에 붙어 들어오므로
 * 백엔드는 직접 볼 수 없다. 프론트가 랜딩 시 이 엔드포인트를 1회 호출해 UTM을 넘겨주면,
 * 백엔드는 first-touch 출처를 쿠키에 박제하고 이후 가입 완료 시 users 테이블에 기록한다.
 */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackingController {

    private final UtmCookie utmCookie;
    private final AnalyticsEventLogger analytics;

    @GetMapping("/visit")
    public ResponseEntity<Void> visit(
            @RequestParam(value = "utm_source", required = false) String source,
            @RequestParam(value = "utm_medium", required = false) String medium,
            @RequestParam(value = "utm_campaign", required = false) String campaign,
            @RequestParam(value = "utm_content", required = false) String content,
            HttpServletRequest request,
            HttpServletResponse response) {

        UtmAttribution utm = UtmAttribution.of(source, medium, campaign, content);
        utmCookie.writeFirstTouch(request, response, utm);

        // 클릭(유입) 자체를 1건의 로그로 적재한다. signup_completed(가입)와 짝지어 전환율을 계산한다.
        analytics.log(AnalyticsEvent.of("landing_visited")
                .put("utm_source", utm.source())
                .put("utm_medium", utm.medium())
                .put("utm_campaign", utm.campaign())
                .put("utm_content", utm.content()));

        return ResponseEntity.noContent().build();
    }
}
