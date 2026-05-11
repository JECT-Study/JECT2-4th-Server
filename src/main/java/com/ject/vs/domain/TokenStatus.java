package com.ject.vs.domain;

public enum TokenStatus {
    VALID,      // 토큰 유효
    EMPTY,      // 토큰 없음
    EXPIRED,    // 만료됨
    INVALID     // 유효하지 않음
}
