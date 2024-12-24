package com.TestExecution.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionListener.class);

    @Autowired
    private TestExecutionService testExecutionService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "TestsQueue")
    public void receiveAnalysisData(String messageBody) {
        try {

            String filename = extractFilename(messageBody);
            String code = extractCode(messageBody);
            String analysisDetails = extractAnalysisDetails(messageBody);
            String tests = extractTests(messageBody);
            String language = extractLanguage(analysisDetails);

            log.info("Received Filename: {}", filename);
            log.info("Received Code:\n{}", code);
            log.info("Received Tests:\n{}", tests);
            log.info("Received Language: {}", language);
            log.info("Received Analysis:\n{}", analysisDetails);


            File tempDir = saveFilesToTempDir(filename, code, tests);


            String testResultsJson = testExecutionService.executeTests(tempDir, language, analysisDetails);


            if (testResultsJson != null && !testResultsJson.isEmpty()) {
                try {

                    ObjectNode messageJson = (ObjectNode) objectMapper.readTree(testResultsJson);


                    messageJson.put("filename", filename);
                    messageJson.put("code", code);


                    String messageToSend = objectMapper.writeValueAsString(messageJson);


                    MessageProperties messageProperties = new MessageProperties();
                    messageProperties.setContentType("application/json");
                    messageProperties.setContentEncoding("UTF-8");
                    Message message = MessageBuilder.withBody(messageToSend.getBytes("UTF-8"))
                            .andProperties(messageProperties)
                            .build();


                    rabbitTemplate.send("recommendation-exchange", "test.execution.message", message);
                    log.info("Test execution results sent to recommendation-exchange with routing key 'test.execution.message': {}", messageToSend);
                } catch (Exception e) {
                    log.error("Error sending test results to recommendation service: ", e);
                }
            } else {
                log.error("Test results are null or empty. Message not sent to recommendation service.");
            }

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

    public String extractTests(String analysisOutput) {
        if (analysisOutput == null || analysisOutput.isEmpty() || !analysisOutput.contains("Tests:")) {
            return "";
        }

        try {
            String testsSection = analysisOutput.split("Tests:")[1];

            int start = testsSection.indexOf("{tests=[");
            if (start == -1) return "";
            start += "{tests=[".length();


            int end = testsSection.lastIndexOf("]}}");
            if (end == -1) return "";

            String testsContent = testsSection.substring(start, end);


            if (testsContent.contains("```java") && testsContent.contains("```")) {
                return testsContent.split("```java")[1].split("```")[0].trim();
            } else {
                return "";
            }
        } catch (Exception e) {
            log.error("Error while extracting tests: ", e);
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
            int endIdx = messageBody.indexOf("\n\nTests:", startIdx);
            return messageBody.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting analysis details: ", e);
            return "";
        }
    }

    private String extractLanguage(String analysisDetails) {
        try {
            if (analysisDetails.contains("\"language\" : \"")) {
                int startIdx = analysisDetails.indexOf("\"language\" : \"") + "\"language\" : \"".length();
                int endIdx = analysisDetails.indexOf("\"", startIdx);
                return analysisDetails.substring(startIdx, endIdx).trim();
            } else {
                return "Unknown";
            }

        } catch (Exception e) {
            log.error("Error extracting language: ", e);
            return "Unknown";
        }
    }

    private File saveFilesToTempDir(String filename, String code, String tests) throws Exception {
        Path tempDir = Files.createTempDirectory("test-execution");
        log.info("Temporary directory created: {}", tempDir.toString());


        String packagePath = extractPackagePath(code);
        String packageName = packagePath.replace("/", ".");


        boolean isDefaultPackage = packageName.equals("default");
        if (isDefaultPackage) {
            packageName = "com.testexecution.generated";
            packagePath = packageName.replace(".", "/");
        }


        File sourceBase = new File(tempDir.toFile(), "src/main/java/" + packagePath);
        File testBase = new File(tempDir.toFile(), "src/test/java/" + packagePath);


        sourceBase.mkdirs();
        testBase.mkdirs();


        File codeFile = new File(sourceBase, filename);
        try (FileWriter writer = new FileWriter(codeFile)) {

            boolean hasPackageDeclaration = code.lines().anyMatch(line -> line.startsWith("package "));
            if (!hasPackageDeclaration) {
                writer.write("package " + packageName + ";\n\n");
            }
            writer.write(code);
        }
        log.info("Code file saved: {}", codeFile.getAbsolutePath());


        String mainClassName = filename.replace(".java", "");


        String testFileName = filename.replaceAll(".java$", "Test.java");
        File testsFile = new File(testBase, testFileName);

        try (FileWriter writer = new FileWriter(testsFile)) {

            writer.write("package " + packageName + ";\n\n");
            writer.write("import " + packageName + "." + mainClassName + ";\n\n");
            writer.write(tests);
        }
        log.info("Test file saved: {}", testsFile.getAbsolutePath());

        return tempDir.toFile();
    }

    private String extractPackagePath(String code) {
        return code.lines()
                .filter(line -> line.startsWith("package "))
                .findFirst()
                .map(line -> line.replace("package ", "").replace(";", "").trim().replace(".", "/"))
                .orElse("default");
    }
}