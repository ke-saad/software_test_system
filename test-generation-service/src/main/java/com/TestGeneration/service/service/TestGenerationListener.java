package com.TestGeneration.service.service;

import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.TestGeneration.service.model.AnalysisResult;
import org.slf4j.Logger;

@Service
public class TestGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationListener.class);
    


    @Autowired
    private TestGenerationService testGenerationService;

    @RabbitListener(queues = "analysisQueue")
    public void receiveAnalysisData(String messageBody) {  
        try {

            // Parse the messageBody to extract the filename, code, and analysis details
            String filename = extractFilename(messageBody);
            String code = extractCode(messageBody);
            String analysisDetails = extractAnalysisDetails(messageBody);


            // Process the extracted data
            testGenerationService.generateTests(analysisDetails, filename, code);

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
