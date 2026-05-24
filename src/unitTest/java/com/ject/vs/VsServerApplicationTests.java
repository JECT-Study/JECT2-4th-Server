package com.ject.vs;

import com.ject.vs.notification.port.out.PushSenderPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class VsServerApplicationTests {

	@MockBean
	PushSenderPort pushSenderPort;

	@Test
	void contextLoads() {
	}

}
