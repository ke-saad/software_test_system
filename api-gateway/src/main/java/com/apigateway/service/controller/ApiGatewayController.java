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
    private String codeAnalysisServiceUrl;

    private final RestTemplate restTemplate;

    public ApiGatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/CodeAnalysisService")
    public ResponseEntity<String> analyzeFile(@RequestParam("file") MultipartFile file) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());


        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);


        String url = codeAnalysisServiceUrl + "/api/v1/analysis/analyzeFile";


        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);


        return response;
    }
}
