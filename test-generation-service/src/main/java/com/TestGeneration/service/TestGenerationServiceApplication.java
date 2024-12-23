package com.TestGeneration.service;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan("com.TestGeneration.service")
public class TestGenerationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestGenerationServiceApplication.class, args);
	}
    @Bean
    public Queue testQueue() {
        return new Queue("analysisQueue", false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("test-generation-exchange");
    }

    @Bean
    public Binding binding(Queue testQueue, TopicExchange exchange) {
        return BindingBuilder.bind(testQueue).to(exchange).with("test.*");
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
