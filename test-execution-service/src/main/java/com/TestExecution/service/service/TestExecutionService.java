package com.TestExecution.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class TestExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);
    @Autowired
    PomGenerator pomGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    public String executeTests(File directory, String language, String analysisDetails) {
        logger.info("Starting test execution for language: {}", language);
        if ("Java".equalsIgnoreCase(language)) {
            generatePomFile(directory, analysisDetails);
            return executeJavaTests(directory, analysisDetails);
        } else {
            String errorMessage = "Test execution for " + language + " is not supported yet. proceeding with no execution";
            logger.error(errorMessage);
            return errorMessage;
        }
    }

    private void generatePomFile(File directory, String analysisDetails) {
        try {
            String projectPath = directory.getAbsolutePath();
            logger.info("Generating POM file for project at: {}", projectPath);
            pomGenerator.generatePom(projectPath);
            logger.info("POM file successfully generated.");
        } catch (Exception e) {
            logger.error("Error generating POM file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate POM file.", e);
        }
    }

    private String executeJavaTests(File directory, String analysisDetails) {
        String projectPath = directory.getAbsolutePath();
        logger.info("Running tests for Java project at: {}", projectPath);
        return runMavenTests(projectPath, analysisDetails);
    }

    private String runMavenTests(String projectPath, String analysisDetails) {
        String command = "cmd /c mvn -f " + projectPath + "\\pom.xml test -e -X";

        try {
            Process process = Runtime.getRuntime().exec(command);

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            Thread outputThread = new Thread(() -> captureStream(process.getInputStream(), output, "OUTPUT"));
            Thread errorThread = new Thread(() -> captureStream(process.getErrorStream(), error, "ERROR"));

            outputThread.start();
            errorThread.start();

            outputThread.join();
            errorThread.join();

            int exitCode = process.waitFor();


            ObjectNode messageJson = objectMapper.createObjectNode();
            messageJson.put("analysisDetails", analysisDetails);
            if (exitCode == 0) {
                messageJson.put("testOutput", "Test execution completed successfully:\n" + output.toString());
            } else {
                messageJson.put("testOutput", "Test execution failed with errors:\n" + error.toString() + "\n" + "Additional output:\n" + output.toString());
            }


            String message = objectMapper.writeValueAsString(messageJson);

            logger.info("Message about to be sent to recommendation service:\n{}", message);
            return message;

        } catch (IOException | InterruptedException e) {
            logger.error("Error during Maven test execution: {}", e.getMessage(), e);
            return "Error during Maven test execution: " + e.getMessage();
        }
    }

    private void captureStream(InputStream stream, StringBuilder builder, String streamName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
                logger.debug("[{}] {}", streamName, line);
            }
        } catch (IOException e) {
            logger.error("Error capturing {} stream: {}", streamName, e.getMessage(), e);
        }
    }
}