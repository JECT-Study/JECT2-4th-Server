package com.ject.vs.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;

/**
 * Integration Test의 공통 설정을 담당하는 추상 클래스.
 *
 * 통합 테스트는 Testcontainers 기반 실제 PostgreSQL 위에서 실행됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final Instant FIXED_NOW = Instant.parse("2025-06-01T12:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("vs_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    protected EntityManager entityManager;

    @MockitoBean
    protected Clock clock;

    @BeforeEach
    void setUpBaseClock() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @BeforeAll
    static void ensureContainerOrSkip() {
        try {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
        } catch (Exception ex) {
            Assumptions.abort("Docker를 사용할 수 없어 Testcontainers 기반 통합 테스트를 건너뜁니다: " + ex.getMessage());
        }
    }
}
