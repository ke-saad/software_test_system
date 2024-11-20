package com.TestGeneration.service.service;

import com.TestGeneration.service.model.AnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    AnalysisResult analysisResult = new AnalysisResult();

    private final String geminiApiKey = "AIzaSyB5Qyxnj30gp5SCstCOkkzo7MoAzI2h3-I";
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-exp-1114:generateContent";

    public TestGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        // Initialize final field here
    }

    public String generateTests(String analysisDetails, String filename, String code) {
    	analysisResult.setFilename(filename);
    	analysisResult.setCodeSource(code);

        try {
            String prompt = createTestGenerationPrompt(analysisDetails);

            // Prepare the request payload
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("role", "user");

            Map<String, String> partMap = new HashMap<>();
            partMap.put("text", prompt);

            roleMap.put("parts", List.of(partMap));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(roleMap));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request to Gemini API
            ResponseEntity<String> response = restTemplate.exchange(
                    geminiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Handle API response
            if (response.getStatusCode().is2xxSuccessful()) {
                // Combine the code, filename, and analysis details into a single string
                String messageBody = String.format(
                	    "Filename: %s\n\n" +
                	    "Code:\n%s\n\n" +
                	    "Analysis:\n%s\n\n" +
                	    "Tests:\n%s \n\n",
                	    filename,
                	    code,
                	    analysisDetails,
                	    parseResponse(analysisDetails, response.getBody(), filename)
                	);

                // Convert the combined string to a byte array
                byte[] messageBodyBytes = messageBody.getBytes("UTF-8");

                // Create MessageProperties and set properties
                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType("text/plain");
                messageProperties.setContentEncoding("UTF-8");

                // Create the message with the combined byte array as the body
                Message message = MessageBuilder.withBody(messageBodyBytes)
                        .andProperties(messageProperties)
                        .build();
                // Send the message

                try {
                    rabbitTemplate.send("TestsQueue", message);
                    log.info("Message sent successfully to TestsQueue.");
                } catch (Exception e) {
                    log.error("Failed to send message to RabbitMQ: ", e);
                }
                
                return "tests generated successfully and sent to the test execution microservice";
            } else {
                log.error("Failed to generate tests. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to generate tests: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while generating tests: ", e);
            throw new RuntimeException("Error while generating tests", e);
        }
    }

    private String createTestGenerationPrompt(String analysisDetails) {
        // Customize this prompt for your use case
        return "Generate the possible tests for the following code analysis result and also the code source and the response should be just like a test file u dont add anything else like exaplanations or just tests in a file format u use all the details provided  in the analysis details and the code source to understand the structure of the methods abd all\n" + analysisDetails;
    }

    private Map<String, Object> parseResponse(String analysisDetails, String responseBody, String filename) {
        try {
            // Parse the response body
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> analysisMap = objectMapper.readValue(analysisDetails, Map.class);
            if (candidates == null || candidates.isEmpty()) {
                log.error("No candidates found in the response");
                throw new RuntimeException("No test cases were retrieved.");
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");


            Map<String, Object> testResponse = new HashMap<>();
            testResponse.put("tests", parts.stream().map(part -> {
                Map<String, String> testDetails = new HashMap<>();
                
                testDetails.put("content", part.get("text"));
                return testDetails;
            }).toList());

            return testResponse;
        } catch (Exception e) {
            log.error("Failed to parse Gemini API response: ", e);
            throw new RuntimeException("Failed to parse Gemini API response", e);
        }
    }

}
