package com.CodeAnalysis.service.controller;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CodeAnalysis.service.model.AnalysisResult;
import com.CodeAnalysis.service.service.CodeAnalysisService;
import org.slf4j.Logger;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/analysis")
public class CodeAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisController.class);

    @Autowired
    private final CodeAnalysisService service;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public CodeAnalysisController(CodeAnalysisService service) {
        this.service = service;
    }

    // POST endpoint to upload a file and analyze the code
    @PostMapping("/analyzeFile")
    public ResponseEntity<?> analyzeFile(@RequestParam("file") MultipartFile file) {
        try {
            // Read the file content
            String code = new String(file.getBytes(), "UTF-8");

            // Format the code as required
            String formattedCode = formatCodeAsJson(code);

            // Call the analyze service with the formatted code
            String analysisDetails = service.analyzeCodeWithGemini(formattedCode);
            String filename = file.getOriginalFilename();

            // Create and populate the AnalysisResult object
            AnalysisResult analysisResult = new AnalysisResult();
            analysisResult.setFileName(filename);
            analysisResult.setAnalysisDetails(analysisDetails);
            analysisResult.setStatus("analyzed");

            // Combine the code, filename, and analysis details into a single string
            String messageBody = String.format("Filename: %s\n\nCode:\n%s\n\nAnalysis:\n%s", filename, code, analysisDetails);

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
            rabbitTemplate.send("analysisQueue", message);

            
            return ResponseEntity.ok().body("Code analysis completed and sent to test generation service");

        } catch (IOException e) {
            log.error("Error reading the file: ", e);
            return ResponseEntity.status(500).body("Error reading the file");
        } catch (Exception e) {
            log.error("Error during code analysis: ", e);
            return ResponseEntity.status(500).body("Error during code analysis");
        }
    }

    // Helper method to format code as a JSON object
    private String formatCodeAsJson(String code) {
        return String.format("{ \"code\": \"%s\" }", escapeJson(code));
    }

    // Helper method to escape special characters for JSON
    private String escapeJson(String code) {
        return code.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
