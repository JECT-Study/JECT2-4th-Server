package com.ject.vs.analytics;

import com.ject.vs.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행동 로그가 실제 PostgreSQL(analytics_events 테이블)에 한 행으로 적재되는지 검증한다.
 *
 * <p>V15 마이그레이션 적용 → 엔티티 매핑 → INSERT 까지 실제 DB로 확인한다.
 * 적재 실패는 {@link AnalyticsEventLogger}가 예외를 삼키므로, 행이 안 생기는 것으로 드러난다.
 */
class AnalyticsEventLoggerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AnalyticsEventLogger analytics;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Test
    void 비회원_이벤트는_properties_JSON과_함께_한_행으로_저장된다() {
        analytics.log(AnalyticsEvent.of("landing_visited")
                .put("utm_source", "instagram")
                .put("utm_campaign", "tangsuyuk"));

        List<AnalyticsEventRecord> rows = analyticsEventRepository.findAll();
        assertThat(rows).hasSize(1);

        AnalyticsEventRecord row = rows.get(0);
        assertThat(row.getEvent()).isEqualTo("landing_visited");
        assertThat(row.isMember()).isFalse();
        assertThat(row.getUserId()).isNull();
        assertThat(row.getOccurredAt()).isEqualTo(FIXED_NOW);
        assertThat(row.getProperties())
                .contains("\"utm_source\":\"instagram\"")
                .contains("\"utm_campaign\":\"tangsuyuk\"");
    }

    @Test
    void userId가_지정되면_회원으로_저장된다() {
        analytics.log(AnalyticsEvent.of("signup_completed")
                .userId(1024L)
                .put("method", "kakao"));

        AnalyticsEventRecord row = analyticsEventRepository.findAll().get(0);
        assertThat(row.getUserId()).isEqualTo(1024L);
        assertThat(row.isMember()).isTrue();
        assertThat(row.getProperties()).contains("\"method\":\"kakao\"");
    }

    @Test
    void 속성이_없으면_properties는_null로_저장된다() {
        analytics.log(AnalyticsEvent.of("simple_event"));

        AnalyticsEventRecord row = analyticsEventRepository.findAll().get(0);
        assertThat(row.getEvent()).isEqualTo("simple_event");
        assertThat(row.getProperties()).isNull();
    }
}
