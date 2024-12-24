package com.emsi.results_recommendations_service.service;

import com.emsi.results_recommendations_service.model.RecommendationResponse;
import com.emsi.results_recommendations_service.model.TestResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RecommendationListener {
    private static final Logger log = LoggerFactory.getLogger(RecommendationListener.class);

    @Autowired
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public RecommendationListener(RecommendationService recommendationService, ObjectMapper objectMapper) {
        this.recommendationService = recommendationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "recommendationQueue")
    public void receiveTestExecutionResults(String messageBody) {
        try {
            log.info("Received message body: {}", messageBody);
            TestResult testResult = parseMessage(messageBody);
            if (testResult != null && testResult.getRecommendations() != null && !testResult.getRecommendations().isEmpty()) {
                RecommendationResponse recommendationResponse = recommendationService.analyzeTestResults(testResult);

                log.info("Test Analysis Completed: {}", recommendationResponse);
                String response = objectMapper.writeValueAsString(recommendationResponse);
                rabbitTemplate.convertAndSend("recommendation-exchange", "recommendation.response", response);
            } else {
                log.info("No recommendations to process or Test result parsing failed.");
            }
        } catch (Exception e) {
            log.error("Error processing test results: ", e);
        }
    }

    private TestResult parseMessage(String messageBody) {
        try {

            JsonNode messageJson = objectMapper.readTree(messageBody);


            String testRunId = messageJson.path("testRunId").asText();
            JsonNode summaryNode = messageJson.path("summary");
            int totalTests = summaryNode.path("totalTests").asInt();
            int passed = summaryNode.path("passed").asInt();
            int failed = summaryNode.path("failed").asInt();
            int skipped = summaryNode.path("skipped").asInt();
            JsonNode recommendationsNode = messageJson.path("recommendations");


            log.info("Test Run ID: {}", testRunId);
            log.info("Summary: totalTests={}, passed={}, failed={}, skipped={}", totalTests, passed, failed, skipped);
            log.info("Recommendations: {}", recommendationsNode);


            return new TestResult(testRunId, totalTests, passed, failed, skipped, recommendationsNode);

        } catch (IOException e) {
            log.error("Error parsing message body: ", e);
            return null;
        }
    }

    private String extractFilename(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Filename: ") + "Filename: ".length();
            if (startIdx < "Filename: ".length()) {
                log.error("Filename not found in the message body");
                return "";
            }
            int endIdx = messageBody.indexOf("\n", startIdx);
            if (endIdx < 0) {
                endIdx = messageBody.length();
            }


            if (startIdx < 0 || endIdx < 0 || startIdx >= endIdx) {
                log.error("Invalid indices for extracting filename: startIdx = {}, endIdx = {}", startIdx, endIdx);
                return "";
            }

            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting filename: ", e);
            return "";
        }
    }

    private String extractCode(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Code:\\n") + "Code:\\n".length();
            int endIdx = messageBody.indexOf("\\n\\nAnalysis:", startIdx);


            if (startIdx < 0 || endIdx < 0 || startIdx >= endIdx) {
                log.error("Invalid indices for extracting code: startIdx = {}, endIdx = {}", startIdx, endIdx);
                return "";
            }

            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting code: ", e);
            return "";
        }
    }

    private String extractAnalysisDetails(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Analysis:\\n") + "Analysis:\\n".length();
            int endIdx = messageBody.indexOf("\\n\\nTests:", startIdx);


            if (startIdx < 0 || endIdx < 0 || startIdx >= endIdx) {
                log.error("Invalid indices for extracting analysis details: startIdx = {}, endIdx = {}", startIdx, endIdx);
                return "";
            }

            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting analysis details: ", e);
            return "";
        }
    }

    private String extractTestOutput(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Test execution completed successfully:\\n") + "Test execution completed successfully:\\n".length();
            return messageBody.substring(startIdx).trim();
        } catch (Exception e) {
            try {
                int startIdx = messageBody.indexOf("Test execution failed with errors:\\n") + "Test execution failed with errors:\\n".length();
                return messageBody.substring(startIdx).trim();
            } catch (Exception exception) {
                log.error("Error extracting Test Output ", e);
                return "";
            }
        }
    }
}
