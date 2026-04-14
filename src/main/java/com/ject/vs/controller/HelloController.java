package com.ject.vs.controller;

import com.ject.vs.dto.HelloResponse;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public HelloResponse hello() {
        return new HelloResponse("Hello, VS Server!", LocalDateTime.now().toString());
    }
}
