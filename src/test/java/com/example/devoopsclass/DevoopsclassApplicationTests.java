package com.example.devoopsclass;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DevoopsclassApplicationTests {

	private final HelloController helloController = new HelloController();

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void homePageShowsCicdDemo() {
		String page = helloController.index();

		assertTrue(page.contains("<title>Space Hyper CI/CD Demo</title>"));
		assertTrue(page.contains("Deployed automatically with Jenkins CI/CD"));
		assertTrue(page.contains("<nav aria-label=\"Route navigation\">"));
		assertTrue(page.contains("<a href=\"/about\">About</a>"));
		assertTrue(page.contains("<a href=\"/contact\">Contact</a>"));
		assertTrue(page.contains("<a href=\"/deploy\">Deploy</a>"));
		assertTrue(page.contains("<a href=\"/binaya\">Binaya</a>"));
	}

	@Test
	void localTestsUseEmbeddedH2() throws Exception {
		try (var connection = dataSource.getConnection()) {
			assertEquals("H2", connection.getMetaData().getDatabaseProductName());
		}
	}

}
