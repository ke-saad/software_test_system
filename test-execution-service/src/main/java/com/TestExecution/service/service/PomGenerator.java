package com.TestExecution.service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PomGenerator {

    private static final Logger log = LoggerFactory.getLogger(PomGenerator.class);

    public void generatePom(String projectRootPath) {
        try {

            String groupId = extractGroupId(projectRootPath);


            String artifactId = Path.of(projectRootPath).getFileName().toString();


            writePomFile(projectRootPath, groupId, artifactId);

        } catch (Exception e) {
            log.error("Error generating POM file", e);
            throw new RuntimeException("Failed to generate POM file.", e);
        }
    }

    private void writePomFile(String projectRootPath, String groupId, String artifactId) {

        String pomContent = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <groupId>" + groupId + "</groupId>\n"
                + "    <artifactId>" + artifactId + "</artifactId>\n"
                + "    <version>1.0-SNAPSHOT</version>\n"
                + "    <dependencies>\n"
                + "        <!-- Testing dependencies -->\n"
                + "        <dependency>\n"
                + "            <groupId>org.junit.jupiter</groupId>\n"
                + "            <artifactId>junit-jupiter</artifactId>\n"
                + "            <version>5.10.0</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.mockito</groupId>\n"
                + "            <artifactId>mockito-core</artifactId>\n"
                + "            <version>4.6.1</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.assertj</groupId>\n"
                + "            <artifactId>assertj-core</artifactId>\n"
                + "            <version>3.24.2</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.springframework.boot</groupId>\n"
                + "            <artifactId>spring-boot-starter-test</artifactId>\n"
                + "            <version>2.7.3</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.junit.vintage</groupId>\n"
                + "            <artifactId>junit-vintage-engine</artifactId>\n"
                + "            <version>5.10.0</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.mockito</groupId>\n"
                + "            <artifactId>mockito-junit-jupiter</artifactId>\n"
                + "            <version>4.6.1</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.testcontainers</groupId>\n"
                + "            <artifactId>testcontainers</artifactId>\n"
                + "            <version>1.19.0</version>\n"
                + "            <scope>test</scope>\n"
                + "        </dependency>\n"

                + "        <!-- Spring Boot dependencies -->\n"
                + "        <dependency>\n"
                + "            <groupId>org.springframework.boot</groupId>\n"
                + "            <artifactId>spring-boot-starter</artifactId>\n"
                + "            <version>2.7.3</version>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.springframework.boot</groupId>\n"
                + "            <artifactId>spring-boot-starter-web</artifactId>\n"
                + "            <version>2.7.3</version>\n"
                + "        </dependency>\n"

                + "        <!-- Database dependencies -->\n"
                + "        <dependency>\n"
                + "            <groupId>org.springframework.boot</groupId>\n"
                + "            <artifactId>spring-boot-starter-data-jpa</artifactId>\n"
                + "            <version>2.7.3</version>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>com.h2database</groupId>\n"
                + "            <artifactId>h2</artifactId>\n"
                + "            <version>2.1.214</version>\n"
                + "        </dependency>\n"

                + "        <!-- JSON Processing -->\n"
                + "        <dependency>\n"
                + "            <groupId>com.fasterxml.jackson.core</groupId>\n"
                + "            <artifactId>jackson-databind</artifactId>\n"
                + "            <version>2.14.2</version>\n"
                + "        </dependency>\n"

                + "        <!-- Cloud dependencies -->\n"
                + "        <dependency>\n"
                + "            <groupId>com.google.cloud</groupId>\n"
                + "            <artifactId>google-cloud-storage</artifactId>\n"
                + "            <version>2.8.0</version>\n"
                + "        </dependency>\n"

                + "        <!-- Logging -->\n"
                + "        <dependency>\n"
                + "            <groupId>ch.qos.logback</groupId>\n"
                + "            <artifactId>logback-classic</artifactId>\n"
                + "            <version>1.2.11</version>\n"
                + "        </dependency>\n"
                + "        <dependency>\n"
                + "            <groupId>org.slf4j</groupId>\n"
                + "            <artifactId>slf4j-api</artifactId>\n"
                + "            <version>2.0.7</version>\n"
                + "        </dependency>\n"

                + "        <!-- Security dependencies -->\n"
                + "        <dependency>\n"
                + "            <groupId>org.springframework.boot</groupId>\n"
                + "            <artifactId>spring-boot-starter-security</artifactId>\n"
                + "            <version>2.7.3</version>\n"
                + "        </dependency>\n"

                + "        <!-- Apache Commons -->\n"
                + "        <dependency>\n"
                + "            <groupId>org.apache.commons</groupId>\n"
                + "            <artifactId>commons-lang3</artifactId>\n"
                + "            <version>3.12.0</version>\n"
                + "        </dependency>\n"

                + "    </dependencies>\n"
                + "</project>";

        try {
            Path pomPath = Paths.get(projectRootPath, "pom.xml");
            Files.write(pomPath, pomContent.getBytes());
            log.info("POM file successfully generated at: {}", pomPath);
        } catch (IOException e) {
            log.error("Failed to write POM file", e);
            throw new RuntimeException("Error writing POM file.", e);
        }
    }

    private String extractGroupId(String projectRootPath) {
        Path srcPath = Path.of(projectRootPath, "src", "main", "java");
        if (Files.exists(srcPath)) {
            try {
                return srcPath.getParent().getParent().getFileName().toString().replace("-", ".");
            } catch (Exception e) {
                log.warn("Unable to determine groupId, using default.");
            }
        }
        return "";
    }
}
