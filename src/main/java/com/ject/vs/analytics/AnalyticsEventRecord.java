package com.ject.vs.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 행동 로그 한 건을 RDB(analytics_events)에 적재하기 위한 엔티티.
 *
 * <p>공통 필드는 컬럼으로, 이벤트별 가변 속성은 {@code properties}(JSON 문자열)에 담는다.
 * 마이그레이션 {@code V15__add_analytics_events.sql} 과 1:1로 대응한다.
 */
@Entity
@Table(name = "analytics_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalyticsEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String event;

    private Long userId;

    @Column(length = 64)
    private String anonymousId;

    @Column(name = "is_member", nullable = false)
    private boolean member;

    @Column(length = 20)
    private String platform;

    @Column(nullable = false)
    private Instant occurredAt;

    /** 이벤트별 속성을 직렬화한 JSON 문자열. 속성이 없으면 null. */
    @Column(columnDefinition = "text")
    private String properties;

    public AnalyticsEventRecord(String event, Long userId, String anonymousId,
                                boolean member, String platform, Instant occurredAt, String properties) {
        this.event = event;
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.member = member;
        this.platform = platform;
        this.occurredAt = occurredAt;
        this.properties = properties;
    }
}
