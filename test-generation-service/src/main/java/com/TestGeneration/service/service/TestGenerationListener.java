package com.TestGeneration.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationListener.class);


    @Autowired
    private TestGenerationService testGenerationService;

    @RabbitListener(queues = "analysisQueue")
    public void receiveAnalysisData(String messageBody) {
        try {
            log.info("starting test generation");


            String filename = extractFilename(messageBody);
            String code = extractCode(messageBody);
            String analysisDetails = extractAnalysisDetails(messageBody);


            testGenerationService.generateTests(analysisDetails, filename, code);

        } catch (Exception e) {
            log.error("Error while processing analysis data: ", e);
        }
    }


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
