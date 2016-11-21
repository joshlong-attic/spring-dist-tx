package com.example;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.*;

@SpringBootApplication
public class XaDsDsApplication {

	private final XADataSourceWrapper wrapper;

	public static void main(String[] args) {
		SpringApplication.run(XaDsDsApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(prefix = "b")
	public DataSource b() throws Exception {
		return xaDataSource("b");
	}

	@Bean
	@ConfigurationProperties(prefix = "a")
	public DataSource a() throws Exception {
		return xaDataSource("a");
	}

	private DataSource xaDataSource(String name) throws Exception {
		JdbcDataSource xaDataSource = new JdbcDataSource();
		xaDataSource.setUrl("jdbc:h2:mem:" + name);
		xaDataSource.setUser("sa");
		xaDataSource.setPassword("");
		return this.wrapper.wrapDataSource(xaDataSource);
	}

	public XaDsDsApplication(XADataSourceWrapper wrapper) {
		this.wrapper = wrapper;
	}

	@Bean
	public DataSourceInitializer bInit(
			@Qualifier("b") DataSource b,
			@Value("classpath:b.sql") Resource r) {
		return init(b, r);
	}

	@Bean
	public DataSourceInitializer aInit(
			@Qualifier("a") DataSource a,
			@Value("classpath:a.sql") Resource r) {
		return init(a, r);
	}

	private DataSourceInitializer init(DataSource ds, Resource r) {
		DataSourceInitializer dsi = new DataSourceInitializer();
		dsi.setDatabasePopulator(new ResourceDatabasePopulator(r));
		dsi.setDataSource(ds);
		return dsi;
	}

	@RestController
	@RequestMapping("/api")
	public static class ApiRestController {

		private final JdbcTemplate a, b;

		public ApiRestController(DataSource a, DataSource b) {
			this.a = new JdbcTemplate(a);
			this.b = new JdbcTemplate(b);
		}

		@GetMapping("/messages")
		public Collection<Map<String, String>> messages() {
			return b.query("select * from MESSAGE", (rs, i) -> {
				Map<String, String> cat = new HashMap<>();
				cat.put("message", rs.getString("MESSAGE"));
				cat.put("id", rs.getString("ID"));
				return cat;
			});
		}

		@GetMapping("/cats")
		public Collection<Map<String, String>> cats() {
			return a.query("select * from CAT", (rs, i) -> {
				Map<String, String> cat = new HashMap<>();
				cat.put("nickname", rs.getString("NICKNAME"));
				cat.put("id", rs.getString("ID"));
				return cat;
			});
		}

		@Transactional
		@PostMapping
		public void write(@RequestBody Map<String, String> payload,
		                  @RequestParam Optional<Boolean> exception) {
			String felix = payload.get("name");

			this.a.update("INSERT INTO CAT( NICKNAME, ID ) VALUES (?,?)",
					felix, UUID.randomUUID().toString());

			this.b.update("INSERT INTO MESSAGE( MESSAGE, ID) VALUES (?,?)",
					"Hello, " + felix + "!", UUID.randomUUID().toString());

			if (exception.orElse(false)) {
				throw new RuntimeException("oops!");
			}
		}
	}
}



