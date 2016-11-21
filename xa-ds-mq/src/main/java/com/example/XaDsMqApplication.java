package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
public class XaDsMqApplication {

	public static final String MSGS = "messages";

	public static void main(String[] args) {
		SpringApplication.run(XaDsMqApplication.class, args);
	}

	@Component
	public static class MessageListener {

		private Log log = LogFactory.getLog(getClass());

		@JmsListener(destination = XaDsMqApplication.MSGS)
		public void messageWritten(String id) {
			log.info("wrote message having id: " + id);
		}
	}

	@RestController
	@RequestMapping("/api")
	public static class ApiRestController {

		private final JmsTemplate jmsTemplate;
		private final JdbcTemplate jdbcTemplate;

		public ApiRestController(JmsTemplate jmsTemplate, JdbcTemplate jdbcTemplate) {
			this.jmsTemplate = jmsTemplate;
			this.jdbcTemplate = jdbcTemplate;
		}

		@GetMapping
		public Collection<Map<String, String>> read() {
			return jdbcTemplate.query("select * from MESSAGE",
					(rs, i) -> {
						Map<String, String> m = new HashMap<>();
						m.put("id", rs.getString("id"));
						m.put("message", rs.getString("message"));
						return m;
					});
		}

		@PostMapping
		@Transactional
		public void write(@RequestBody Map<String, String> payload,
		                  @RequestParam Optional<Boolean> exception) {


			String id = UUID.randomUUID().toString();

			this.jdbcTemplate.update(
					"insert into MESSAGE(MESSAGE, id) VALUES(?,?)",
					payload.get("message"), id);

			this.jmsTemplate.convertAndSend(XaDsMqApplication.MSGS, id);

			if (exception.orElse(false)) {
				throw new RuntimeException("Nope!");
			}
		}
	}

}