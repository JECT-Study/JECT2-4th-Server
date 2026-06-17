package com.ject.vs.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;

/**
 * Integration Test의 공통 설정을 담당하는 추상 클래스.
 *
 * 통합 테스트는 Testcontainers 기반 실제 PostgreSQL 위에서 실행됩니다.
 *
 * <p>컨테이너는 JUnit({@code @Testcontainers}) 라이프사이클에 맡기지 않고
 * 정적 초기화 블록에서 한 번만 시작하는 싱글톤 패턴을 사용합니다.
 * Spring이 ApplicationContext를 캐싱해 여러 테스트 클래스에 재사용하는데,
 * 클래스 단위로 컨테이너를 내리면 캐시된 컨텍스트가 이미 중지된 컨테이너를
 * 가리켜 {@code ConnectException}이 발생하기 때문입니다. 한 번 시작한 컨테이너는
 * JVM 종료 시 Ryuk가 정리합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected static final Instant FIXED_NOW = Instant.parse("2025-06-01T12:00:00Z");

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("vs_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

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
