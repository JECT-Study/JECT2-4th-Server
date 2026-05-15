package com.ject.vs.common.adapter.web;

import com.ject.vs.common.adapter.web.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public HelloResponse hello() {
        return new HelloResponse("Hello, VS Server!", Instant.now().toString());
    }
}
