package com.emsi.results_recommendations_service.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue recommendationQueue() {
        return new Queue("recommendationQueue", true);
    }

    @Bean
    public TopicExchange recommendationExchange() {
        return new TopicExchange("recommendation-exchange");
    }

    @Bean
    public Binding recommendationBinding(Queue recommendationQueue, TopicExchange recommendationExchange) {
        return BindingBuilder.bind(recommendationQueue).to(recommendationExchange).with("test.execution.message");
    }

}
