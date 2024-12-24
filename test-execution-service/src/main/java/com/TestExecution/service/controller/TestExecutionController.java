package com.TestExecution.service.controller;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestExecutionController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/sendTestMessage")
    public String sendTestMessage(@RequestBody String messageBody) {
        try {
            System.out.println("Preparing to send message to RabbitMQ.");


            byte[] messageBodyBytes = messageBody.getBytes("UTF-8");


            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("text/plain");
            messageProperties.setContentEncoding("UTF-8");


            Message message = MessageBuilder.withBody(messageBodyBytes)
                    .andProperties(messageProperties)
                    .build();


            rabbitTemplate.send("recommendation-exchange", "test.execution.message", message);

            System.out.println("Message sent successfully to recommendation-exchange with routing key 'test.execution.message'.");
            return "Message sent!";
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
            return "Error sending message: " + e.getMessage();
        }
    }
}
