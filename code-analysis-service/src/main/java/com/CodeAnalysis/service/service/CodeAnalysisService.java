package com.CodeAnalysis.service.service;

import com.CodeAnalysis.service.controller.CodeAnalysisController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CodeAnalysisService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CodeAnalysisController.class);
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey = "AIzaSyB5Qyxnj30gp5SCstCOkkzo7MoAzI2h3-I";
    private final String geminiApiKey1 = "AIzaSyCyM3spn20mTVhFwR6veMrz235oeh_rsrc";
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-exp-1114:generateContent";

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
            log.info("Attempt {} - Gemini API Key being used: {}", (attempt + 1), headers.get("x-goog-api-key"));
            log.debug("Attempt {} - Gemini API request body:\n{}", (attempt + 1), requestBody);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        geminiApiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("API Response (Attempt {}): {}", (attempt + 1), response.getBody());
                    log.debug("Attempt {} - Gemini API response headers: {}", (attempt + 1), response.getHeaders());
                    return parseResponse(response.getBody());
                } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.error("Attempt {}: Rate limited (HTTP 429). Retrying with secondary API key...", (attempt + 1));
                    headers.set("x-goog-api-key", geminiApiKey1);
                    log.info("Attempt {} - Changing Gemini API Key to: {}", (attempt + 1), headers.get("x-goog-api-key"));
                } else {
                    log.error("Attempt {}: Error - Status code {}", (attempt + 1), response.getStatusCode());
                    return "Error: Received non-OK response.";
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.error("Attempt {}: Rate limited (HTTP 429). Retrying with secondary API key...", (attempt + 1));
                    headers.set("x-goog-api-key", geminiApiKey1);
                    log.info("Attempt {} - Changing Gemini API Key to: {}", (attempt + 1), headers.get("x-goog-api-key"));
                } else {
                    log.error("Attempt {}: API request failed - {}", (attempt + 1), e.getMessage());
                    log.error("Attempt {} - Exception message:\n{}", (attempt + 1), e.getMessage());
                }
            }

            try {
                TimeUnit.SECONDS.sleep(RETRY_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return "Error: All attempts to connect to the API failed.";
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String parseResponse(String responseBody) {
        try {
            log.info("Parsing the response: {}", responseBody);

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            if (responseMap == null || !responseMap.containsKey("candidates")) {
                log.error("Invalid response format: missing 'candidates'.");
                return "Error: Invalid response format.";
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates.isEmpty()) {
                log.error("No candidates found in the response.");
                return "Error: No meaningful analysis was retrieved.";
            }

            Map<String, Object> candidate = candidates.get(0);
            if (!candidate.containsKey("content")) {
                log.error("Missing 'content' field in candidate.");
                return "Error: Invalid candidate format.";
            }

            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            if (!content.containsKey("parts")) {
                log.error("Missing 'parts' field in content.");
                return "Error: Invalid content format.";
            }

            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
            if (parts.isEmpty() || !parts.get(0).containsKey("text")) {
                log.error("Missing 'text' field in parts.");
                return "Error: Invalid parts format.";
            }

            String rawText = parts.get(0).get("text");
            String jsonText = rawText.replace("```json", "").replace("```", "").trim();

            if (jsonText.isEmpty()) {
                log.error("Empty JSON content in the response.");
                return "Error: Empty analysis content.";
            }

            Map<String, Object> analysisData = objectMapper.readValue(jsonText, Map.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysisData);

        } catch (IOException e) {
            log.error("Failed to parse Gemini API response: " + e.getMessage(), e);
            return "Error: Failed to parse response.";
        }
    }
}
