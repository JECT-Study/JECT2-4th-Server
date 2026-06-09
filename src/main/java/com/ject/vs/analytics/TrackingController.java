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

    @GetMapping("/visit")
    public ResponseEntity<Void> visit(
            @RequestParam(value = "utm_source", required = false) String source,
            @RequestParam(value = "utm_medium", required = false) String medium,
            @RequestParam(value = "utm_campaign", required = false) String campaign,
            @RequestParam(value = "utm_content", required = false) String content,
            HttpServletRequest request,
            HttpServletResponse response) {

        utmCookie.writeFirstTouch(request, response, UtmAttribution.of(source, medium, campaign, content));
        return ResponseEntity.noContent().build();
    }
}
