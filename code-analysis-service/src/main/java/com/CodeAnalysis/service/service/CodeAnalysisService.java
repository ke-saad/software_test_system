package com.CodeAnalysis.service.service;

import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.CodeAnalysis.service.controller.CodeAnalysisController;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CodeAnalysisService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CodeAnalysisController.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String geminiApiKey = "AIzaSyB5Qyxnj30gp5SCstCOkkzo7MoAzI2h3-I";
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-exp-1114:generateContent";

    // Retry settings
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5; // seconds

    public CodeAnalysisService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String buildPromptText() {
        return "Analyze the following code and provide a structured JSON response with these details:\n"
                + "- `language`: Programming language.\n"
                + "- `imports`: List of import statements or dependencies.\n"
                + "- `classes`: For each class:\n"
                + "  - `name`, `attributes` (type, description), `methods` (name, parameters, return type, description).\n"
                + "- `functions`: Standalone functions with details (parameters, return type, description).\n"
                + "- `objects`: Objects instantiated, their class, and attributes.\n"
                + "- `called_functions`: Functions called, including caller context.\n"
                + "- `comments`: Any inline or block comments explaining the code.\n"
                + "- `overall_structure`: Summary of the code's structure and purpose.\n"
                + "- `potential_bugs`: Any identified bugs, inefficiencies, or improvements.\n"
                + "- `test_suggestions`: High-level suggestions for unit or integration tests.\n"
                + "Please ensure the response is complete, accurate, and well-structured in JSON format.";
    }

    public String analyzeCodeWithGemini(String code) {
        String prompt = buildPromptText();
        String requestBody = "{\"contents\":[{\"role\": \"user\", \"parts\":[{\"text\": \"" + prompt + " "
                + escapeJson(code) + "\"}]}]}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        geminiApiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return parseResponse(response.getBody());
                } else {
                    log.error("Error: Status code " + response.getStatusCode());
                    return null;
                }
            } catch (HttpClientErrorException e) {
                log.error("Attempt " + (attempt + 1) + ": API request failed - " + e.getMessage());
            }

            try {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return null; // All attempts failed
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")   // Escape backslashes first
                    .replace("\"", "\\\"")   // Escape double quotes
                    .replace("\n", "\\n")    // Newline
                    .replace("\r", "\\r")    // Carriage return
                    .replace("\t", "\\t");   // Tab
    }

    private String parseResponse(String responseBody) {
        try {

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.error("No candidates found in the response");
                return "No meaningful analysis was retrieved.";
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
            String rawText = parts.get(0).get("text");

            String jsonText = rawText.replace("```json", "").replace("```", "").trim();

            Map<String, Object> analysisData = objectMapper.readValue(jsonText, Map.class);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysisData);

        } catch (IOException e) {
            log.error("Failed to parse Gemini API response: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse Gemini API response", e);
        }
    }
}
