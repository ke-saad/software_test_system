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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String geminiApiKey = "AIzaSyB5Qyxnj30gp5SCstCOkkzo7MoAzI2h3-I";
    private final String geminiApiKey1 = "AIzaSyCyM3spn20mTVhFwR6veMrz235oeh_rsrc";
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-exp-1114:generateContent";

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5; // seconds

    public TestGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateTests(String analysisDetails, String filename, String code) {
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setFilename(filename);
        analysisResult.setCodeSource(code);

        String prompt = createTestGenerationPrompt(analysisDetails);
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

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        geminiApiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    String messageBody = String.format(
                            "Filename: %s\n\nCode:\n%s\n\nAnalysis:\n%s\n\nTests:\n%s\n\n",
                            filename,
                            code,
                            analysisDetails,
                            parseResponse(analysisDetails, response.getBody(), filename)
                    );

                    byte[] messageBodyBytes = messageBody.getBytes("UTF-8");
                    MessageProperties messageProperties = new MessageProperties();
                    messageProperties.setContentType("text/plain");
                    messageProperties.setContentEncoding("UTF-8");
                    Message message = MessageBuilder.withBody(messageBodyBytes)
                            .andProperties(messageProperties)
                            .build();
                    rabbitTemplate.send("TestsQueue", message);
                    log.info("Message sent successfully to TestsQueue."+(String)messageBody);
                    return "Tests generated successfully and sent to the test execution microservice.";
                } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.error("Attempt {}: Rate limited (HTTP 429). Retrying with secondary API key...", (attempt + 1));
                    headers.set("x-goog-api-key", geminiApiKey1);
                } else {
                    log.error("Attempt {}: Error - Status code {}", (attempt + 1), response.getStatusCode());
                    return null;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.error("Attempt {}: Rate limited (HTTP 429). Retrying with secondary API key...", (attempt + 1));
                    headers.set("x-goog-api-key", geminiApiKey1);
                } else {
                    log.error("Attempt {}: API request failed - {}", (attempt + 1), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Attempt {}: Unexpected error occurred: {}", (attempt + 1), e.getMessage());
            }

            try {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return null; // All attempts failed
    }

    private String createTestGenerationPrompt(String analysisDetails) {
        return "Generate possible tests for the following code analysis result and code source. Response should only include tests in a file format and u be as accurate as u can and u make sure to avoid any mismatches and typos because its crucial for the test to run u implement exactly what is needed and mentioned in tests analysis attached to this message,also the test file should include any necessary import for any necessaryclass that exists in the analysis sent with this prompt u generate a full working test file:\n" + analysisDetails;
    }

    private Map<String, Object> parseResponse(String analysisDetails, String responseBody, String filename) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
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
