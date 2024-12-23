package com.TestExecution.service;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TestExecutionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestExecutionServiceApplication.class, args);
	}
    @Bean
    public Queue testsQueue() {
        return new Queue("TestsQueue", true);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
