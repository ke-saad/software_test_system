package com.TestExecution.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    @RabbitListener(queues = "TestsQueue")
    public void receiveAnalysisData(String messageBody) {
        try {
            // Extract relevant data
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

            // Save files to a temporary directory
            File tempDir = saveFilesToTempDir(filename, code, tests);

            // Pass the directory and language to the service
            testExecutionService.executeTests(tempDir, language, analysisDetails);

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
        if (analysisOutput == null || analysisOutput.isEmpty()) {
            return "";
        }

        try {
            String testsSection = analysisOutput.split("Tests:")[1];
            String testsContent = testsSection.split("\\{tests=\\[")[1].split("]}")[0];
            String testCode = testsContent.split("```java")[1].split("```")[0].trim();
            return testCode;
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
            int startIdx = analysisDetails.indexOf("\"language\" : \"") + "\"language\" : \"".length();
            int endIdx = analysisDetails.indexOf("\"", startIdx);
            return analysisDetails.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.error("Error extracting language: ", e);
            return "Unknown";
        }
    }

    private File saveFilesToTempDir(String filename, String code, String tests) throws Exception {
        Path tempDir = Files.createTempDirectory("test-execution");
        log.info("Temporary directory created: {}", tempDir.toString());

        // Parse the package path and name from the code
        String packagePath = extractPackagePath(code);
        String packageName = packagePath.replace("/", ".");

        // Use a fallback package if no package is declared
        boolean isDefaultPackage = packageName.equals("default");
        if (isDefaultPackage) {
            packageName = "com.testexecution.generated";
            packagePath = packageName.replace(".", "/");
        }

        // Base paths for source and test directories
        File sourceBase = new File(tempDir.toFile(), "src/main/java/" + packagePath);
        File testBase = new File(tempDir.toFile(), "src/test/java/" + packagePath);

        // Create directories
        sourceBase.mkdirs();
        testBase.mkdirs();

        // Save the main code file
        File codeFile = new File(sourceBase, filename);
        try (FileWriter writer = new FileWriter(codeFile)) {
            // Check if a package declaration exists in the code
            boolean hasPackageDeclaration = code.lines().anyMatch(line -> line.startsWith("package "));
            if (!hasPackageDeclaration) {
                writer.write("package " + packageName + ";\n\n");
            }
            writer.write(code);
        }
        log.info("Code file saved: {}", codeFile.getAbsolutePath());

        // Extract the main class name
        String mainClassName = filename.replace(".java", "");

        // Save the test file
        String testFileName = filename.replaceAll(".java$", "Test.java");
        File testsFile = new File(testBase, testFileName);

        try (FileWriter writer = new FileWriter(testsFile)) {
            // Add package declaration to the test file
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
