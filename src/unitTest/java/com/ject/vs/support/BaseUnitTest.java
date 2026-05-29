package com.ject.vs.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit Test의 공통 설정을 담당하는 추상 클래스.
 * 현재는 MockitoExtension만 적용하고 있으며,
 * 향후 공통 Mock 설정이나 유틸리티가 필요해지면 이 클래스에 추가한다.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}
