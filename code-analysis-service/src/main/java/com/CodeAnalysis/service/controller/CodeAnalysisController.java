package com.CodeAnalysis.service.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;
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

    public CodeAnalysisController(CodeAnalysisService service) {
        this.service = service;
    }

    // POST endpoint to upload a file and analyze the code
    @PostMapping("/analyzeFile")
    public ResponseEntity<String> analyzeFile(@RequestParam("file") MultipartFile file) {
        try {
            // Read the file content
            String code = new String(file.getBytes());

            // Format the code as required
            String formattedCode = formatCodeAsJson(code);

            // Call the analyze service with the formatted code
            String analysisDetails = service.analyzeCodeWithGemini(formattedCode);

            // Create and populate the AnalysisResult object
            AnalysisResult analysisResult = new AnalysisResult();
            analysisResult.setFileName(file.getOriginalFilename()); // Use the original file name
            analysisResult.setAnalysisDetails(analysisDetails);
            analysisResult.setStatus("analyzed");

            // Log and return the AnalysisResult object
            log.info("AnalysisResult created: " + analysisResult);
            return ResponseEntity.ok(analysisResult.getAnalysisDetails());

        } catch (IOException e) {
            log.error("Error reading the file: ", e);
            return ResponseEntity.status(500).body(null);
        } catch (Exception e) {
            log.error("Error during code analysis: ", e);
            return ResponseEntity.status(500).body(null);
        }
    }


    // Helper method to format code as a JSON object
    private String formatCodeAsJson(String code) {
        // Return the code in the format required
        return String.format("{ \"code\": \"%s\" }", escapeJson(code));
    }

    // Helper method to escape special characters for JSON
    private String escapeJson(String code) {
        return code.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
