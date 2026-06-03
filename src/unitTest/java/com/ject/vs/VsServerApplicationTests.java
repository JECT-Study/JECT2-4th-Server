package com.ject.vs;

import com.ject.vs.image.port.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class VsServerApplicationTests {

	@MockitoBean
	private ImageService imageService;

	@Test
	void contextLoads() {
	}

}
