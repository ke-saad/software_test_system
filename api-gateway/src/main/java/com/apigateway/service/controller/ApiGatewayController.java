package com.apigateway.service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api-gateway")
public class ApiGatewayController {

    @Value("${code-analysis-service.url}")
    private String codeAnalysisServiceUrl;  // Inject the URL of the Code Analysis service

    private final RestTemplate restTemplate;

    public ApiGatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/CodeAnalysisService")
    public ResponseEntity<String> analyzeFile(@RequestParam("file") MultipartFile file) {
        // Prepare the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create a MultiValueMap to hold the file and other parameters
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());  // Add the MultipartFile to the body
        
        // Wrap the body and headers in an HttpEntity
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        // Define the URL of the target microservice (Code Analysis service)
        String url = codeAnalysisServiceUrl + "/api/v1/analysis/analyzeFile";

        // Make the POST request to the microservice
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        // Return the response from the Code Analysis service
        return response;
    }
}
