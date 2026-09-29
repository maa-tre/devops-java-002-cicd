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
		assertTrue(page.contains("width: 11ch"));
		assertTrue(page.contains("href=\"/\" aria-current=\"page\""));
		assertTrue(page.contains("aria-label=\"Main navigation\""));
	}

	@Test
	void allDemoRoutesUseTheSharedNavigationAndKeepTheirContent() {
		assertRoute(new AboutController().index(), "/about", "A small app with a real delivery pipeline.");
		assertRoute(new ContactController().index(), "/contact", "Thanks for stopping by.");
		assertRoute(new DeployController().index(), "/deploy", "From source code to EC2.");
		assertRoute(new BinayaController().index(), "/binaya", "Binaya's project page.");
	}

	private void assertRoute(String page, String route, String heading) {
		assertTrue(page.contains("href=\"" + route + "\" aria-current=\"page\""));
		assertTrue(page.contains("aria-label=\"Main navigation\""));
		assertTrue(page.contains("<h1>" + heading + "</h1>"));
	}

}
