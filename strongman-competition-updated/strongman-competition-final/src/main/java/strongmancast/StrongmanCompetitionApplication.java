package strongmancast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class StrongmanCompetitionApplication {

	public static void main(String[] args) {
		configureDatabaseUrlFromHost();
		SpringApplication.run(StrongmanCompetitionApplication.class, args);
	}

	private static void configureDatabaseUrlFromHost() {
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("SPRING_DATASOURCE_URL") != null) {
			return;
		}

		URI databaseUri = URI.create(databaseUrl);
		String userInfo = databaseUri.getUserInfo();
		if (userInfo == null || !userInfo.contains(":")) {
			return;
		}

		String[] credentials = userInfo.split(":", 2);
		int port = databaseUri.getPort() == -1 ? 5432 : databaseUri.getPort();
		String jdbcUrl = "jdbc:postgresql://" + databaseUri.getHost()
				+ ":" + port
				+ databaseUri.getPath();

		if (databaseUri.getQuery() != null && !databaseUri.getQuery().isBlank()) {
			jdbcUrl += "?" + databaseUri.getQuery();
		}

		System.setProperty("spring.datasource.url", jdbcUrl);
		System.setProperty("spring.datasource.username", decode(credentials[0]));
		System.setProperty("spring.datasource.password", decode(credentials[1]));
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}

