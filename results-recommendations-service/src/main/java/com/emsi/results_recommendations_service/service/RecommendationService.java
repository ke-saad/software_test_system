package com.emsi.results_recommendations_service.service;

import com.emsi.results_recommendations_service.model.Recommendation;
import com.emsi.results_recommendations_service.model.RecommendationResponse;
import com.emsi.results_recommendations_service.model.Summary;
import com.emsi.results_recommendations_service.model.TestResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final Map<String, RecommendationResponse> recommendationCache = new ConcurrentHashMap<>();
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    ;
    private String geminiApiKey = "AIzaSyB5Qyxnj30gp5SCstCOkkzo7MoAzI2h3-I";
    ;
    private String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-exp-1114:generateContent";

    public RecommendationResponse analyzeTestResults(TestResult testResult) {
        String testRunId = UUID.randomUUID().toString();


        List<Recommendation> recommendations = generateRecommendationsWithGemini(
                testResult.getTestOutput(),
                testResult.getCode(),
                testResult.getAnalysisDetails()
        );


        RecommendationResponse response = new RecommendationResponse();
        response.setTestRunId(testRunId);
        response.setSummary(new Summary(testResult.getTotalTests(), testResult.getPassed(), testResult.getFailed(), testResult.getSkipped()));
        response.setRecommendations(recommendations);


        recommendationCache.put(testRunId, response);

        return response;
    }

    public RecommendationResponse getRecommendationById(String testRunId) {
        return recommendationCache.get(testRunId);
    }

    private List<Recommendation> generateRecommendationsWithGemini(String testOutput, String code, String analysisDetails) {
        try {
            String prompt = buildRecommendationPrompt(testOutput, code, analysisDetails);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);


            String requestBody = "{\"contents\":[{\"role\": \"user\", \"parts\":[{\"text\": \"" + escapeJson(prompt) + "\"}]}]}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);


            ResponseEntity<String> response = restTemplate.exchange(
                    geminiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGeminiResponse(response.getBody());
            } else {
                log.error("Error from Gemini API: Status code {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String buildRecommendationPrompt(String testOutput, String code, String analysisDetails) {

        return "Given the following test output:\n" + testOutput +
                "\n\nAnd the following code:\n" + code +
                "\n\nAnd the following analysis details:\n" + analysisDetails +
                "\n\nPlease provide recommendations to improve the code and tests. Consider code quality, test coverage, and potential bugs.";
    }

    private List<Recommendation> parseGeminiResponse(String responseBody) {
        try {


            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                List<Recommendation> recommendations = new ArrayList<>();
                if (parts.isArray()) {
                    for (JsonNode part : parts) {
                        String text = part.path("text").asText("");


                        String[] extractedRecommendations = text.split("\\n\\s*\\n");
                        for (String extractedRecommendation : extractedRecommendations) {
                            Recommendation recommendation = new Recommendation();
                            recommendation.setType("AI-Generated");
                            recommendation.setMessage(extractedRecommendation.trim());

                            recommendations.add(recommendation);
                        }
                    }
                }

                return recommendations;
            } else {
                log.warn("No candidates found in the Gemini API response.");
                return Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing Gemini API response", e);
            return Collections.emptyList();
        }
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private int countLines(String methodBody) {
        String[] lines = methodBody.split("\n");
        return lines.length;
    }

    private String extractMethodBody(String code, String methodName) {
        String methodPattern = ".*(?:public|private|protected)\\s+\\w+\\s+" + methodName + "\\s*\\((.*?)\\)\\s*(?:throws\\s+\\w+(?:,\\s+\\w+)*)?\\s*\\{(.*?)\\}.*";
        Pattern pattern = Pattern.compile(methodPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }
}