package com.example.devoopsclass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DevoopsclassApplicationTests {

	private final HelloController helloController = new HelloController();

	@Test
	void contextLoads() {
	}

	@Test
	void homePageShowsCicdDemo() {
		String page = helloController.index();

		assertTrue(page.contains("<title>Space Hyper CI/CD Demo</title>"));
		assertTrue(page.contains("Space Hyper and Amar Rana"));
		assertTrue(page.contains("letter--space"));
		assertTrue(page.contains("letter--amar"));
		assertTrue(page.contains("prefers-reduced-motion: reduce"));
		assertTrue(page.contains("Java application is running on AWS EC2."));
	}

}
