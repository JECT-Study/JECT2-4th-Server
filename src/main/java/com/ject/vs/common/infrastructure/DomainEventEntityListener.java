package com.ject.vs.common.infrastructure;

import com.ject.vs.common.domain.AggregateRoot;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

public class DomainEventEntityListener {

    @PostPersist
    @PostUpdate
    public void onSave(Object entity) {
        if (entity instanceof AggregateRoot root) {
            root.getDomainEvents().forEach(DomainEventPublisher::publish);
            root.clearDomainEvents();
        }
    }
}
