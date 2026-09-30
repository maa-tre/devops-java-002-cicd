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
	void deployPageExplainsPinProtectedRollback() {
		RollbackController controller = new RollbackController(
				org.mockito.Mockito.mock(RollbackRequestRepository.class),
				new RollbackPinVerifier(""),
				"http://127.0.0.1:8790",
				"");
		String page = controller.page();

		assertTrue(page.contains("Available image"));
		assertTrue(page.contains("Rollback PIN"));
		assertTrue(page.contains("up to 60 seconds"));
		assertTrue(page.contains("If the target is unhealthy"));
		assertTrue(page.contains(":8081/deploy"));
		assertTrue(page.contains("health_check"));
		assertTrue(page.contains("/api/rollback"));
		assertTrue(page.contains("Recent rollback activity"));
	}

	@Test
	void localTestsUseEmbeddedH2() throws Exception {
		try (var connection = dataSource.getConnection()) {
			assertEquals("H2", connection.getMetaData().getDatabaseProductName());
		}
	}

}
