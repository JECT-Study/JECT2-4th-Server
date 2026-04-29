package com.ject.vs.common.domain;

import java.util.List;

public interface AggregateRoot {
    List<Object> getDomainEvents();
    void clearDomainEvents();
}
