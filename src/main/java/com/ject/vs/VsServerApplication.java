package com.ject.vs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VsServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VsServerApplication.class, args);
	}

}
