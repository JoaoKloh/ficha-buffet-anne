package com.jbkloh.ficha_buffet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FichaBuffetApplicationTests {

	@DynamicPropertySource
	static void chavesRsa(DynamicPropertyRegistry registry) {
		TestRsaKeys.registrar(registry);
	}

	@Test
	void contextLoads() {
	}

}
