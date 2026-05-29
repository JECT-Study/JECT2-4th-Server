package com.ject.vs.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;

/**
 * Integration Test의 공통 설정을 담당하는 추상 클래스.
 * - Spring Context 로드
 * - Test 프로필 활성화
 * - 트랜잭션 롤백
 * - Clock 고정 (테스트에서 시간에 의존하는 로직을 제어하기 위함)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final Instant FIXED_NOW = Instant.parse("2025-06-01T12:00:00Z");

    @Autowired
    protected EntityManager entityManager;

    @MockitoBean
    protected Clock clock;

    @BeforeEach
    void setUpBaseClock() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }
}
