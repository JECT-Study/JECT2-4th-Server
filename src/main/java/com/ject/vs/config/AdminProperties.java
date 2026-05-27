package com.ject.vs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "admin")
public record AdminProperties(
        List<Long> userIds
) {
}
