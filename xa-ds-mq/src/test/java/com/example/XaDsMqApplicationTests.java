package com.example;

import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class XaDsMqApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private String msg = "Hello, world!";

	@Test
	public void writeSucceeds() throws Exception {

		this.mockMvc.perform(
				post("/api")
						.content("{ \"message\":\"" + msg + "\" }")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		this.mockMvc.perform(MockMvcRequestBuilders.get("/api"))
				.andExpect(status().isOk())
				.andExpect(content().json(
						"[{ \"message\": \"" + msg + "\" }]"
				));

	}

	@Test
	public void writeFails() throws Exception {

		try {
			this.mockMvc.perform(
					post("/api?exception=true")
							.content("{ \"message\":\"" + msg + "\" }")
							.contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is5xxServerError());

			Assert.fail();

		} catch (Exception ex) {
			LogFactory.getLog(getClass()).info("exception!" +
					ex.getLocalizedMessage());
		}

		this.mockMvc.perform(MockMvcRequestBuilders.get("/api"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));

	}

}
