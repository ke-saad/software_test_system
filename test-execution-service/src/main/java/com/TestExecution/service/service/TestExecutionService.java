package com.TestExecution.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class TestExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);
    @Autowired
    PomGenerator pomGenerator;

    public String executeTests(File directory, String language, String analysisDetails) {
        logger.info("Starting test execution for language: {}", language);

        if ("Java".equalsIgnoreCase(language)) {
            generatePomFile(directory, analysisDetails); // Generate the POM file
            return executeJavaTests(directory); // Execute Java tests
        } else {
            String errorMessage = "Test execution for " + language + " is not supported yet.";
            logger.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
    }

    private void generatePomFile(File directory, String analysisDetails) {
        try {
            String projectPath = directory.getAbsolutePath();
            logger.info("Generating POM file for project at: {}", projectPath);

			pomGenerator.generatePom(projectPath); // Assuming you have this utility

            logger.info("POM file successfully generated.");
        } catch (Exception e) {
            logger.error("Error generating POM file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate POM file.", e);
        }
    }

    private String executeJavaTests(File directory) {
        String projectPath = directory.getAbsolutePath();
        logger.info("Running tests for Java project at: {}", projectPath);
        return runMavenTests(projectPath); // Execute Maven tests
    }

    private String runMavenTests(String projectPath) {
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

            if (exitCode == 0) {
                logger.info("Test execution completed successfully:\n{}", output);
                return "Test execution completed successfully:\n" + output;
            } else {
                logger.error("Test execution failed:\n{}", error);
                logger.error("Additional output:\n{}", output);
                return "Test execution failed with errors:\n" + error;
            }
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
                logger.debug("[{}] {}", streamName, line); // Log each line
            }
        } catch (IOException e) {
            logger.error("Error capturing {} stream: {}", streamName, e.getMessage(), e);
        }
    }
}
