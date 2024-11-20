package com.TestExecution.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class TestExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(TestExecutionListener.class);
    


    @Autowired
    private TestExecutionService testExecutionService;

    @RabbitListener(queues = "TestsQueue")
    public void receiveAnalysisData(String messageBody) {  
        try {

            // Parse the messageBody to extract the filename, code, and analysis details
            String filename = extractFilename(messageBody);
            String code = extractCode(messageBody);
            String analysisDetails = extractAnalysisDetails(messageBody);
            String tests = extractTests(messageBody);

            log.info("Received Filename: {}", filename);
            log.info("Received Code:\n{}", code);
            log.info("Received Tests:\n{}", tests);
            log.info("Received Analysis:\n{}", analysisDetails);

            // Process the extracted data

        } catch (Exception e) {
            log.error("Error while processing analysis data: ", e);
        }
    }

    // Helper method to extract filename from the message body
    private String extractFilename(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Filename: ") + "Filename: ".length();
            int endIdx = messageBody.indexOf("\n", startIdx);
            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting filename: ", e);
            return "";
        }
    }
    private String extractTests(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Tests: ") + "Tests: ".length();
            int endIdx = messageBody.indexOf("\n", startIdx);
            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting tests: ", e);
            return "";
        }
    }

    // Helper method to extract code from the message body
    private String extractCode(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Code:\n") + "Code:\n".length();
            int endIdx = messageBody.indexOf("\n\nAnalysis:", startIdx);
            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting code: ", e);
            return "";
        }
    }

    // Helper method to extract analysis details from the message body
    private String extractAnalysisDetails(String messageBody) {
        try {
            int startIdx = messageBody.indexOf("Analysis:\n") + "Analysis:\n".length();
            return messageBody.substring(startIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting analysis details: ", e);
            return "";
        }
    }
}
