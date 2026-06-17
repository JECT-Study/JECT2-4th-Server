package com.ject.vs.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventRecord, Long> {
}
