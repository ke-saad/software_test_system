package com.emsi.results_recommendations_service.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TestResult {
    private String testRunId;
    private int totalTests;
    private int passed;
    private int failed;
    private int skipped;
    private JsonNode recommendations;


    public TestResult(String testRunId, int totalTests, int passed, int failed, int skipped) {
        this.testRunId = testRunId;
        this.totalTests = totalTests;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
    }


    public TestResult(String testRunId, int totalTests, int passed, int failed, int skipped, JsonNode recommendations) {
        this.testRunId = testRunId;
        this.totalTests = totalTests;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.recommendations = recommendations;
    }

    public String getTestOutput() {
        if (recommendations != null && recommendations.has("testOutput")) {
            return recommendations.get("testOutput").asText();
        }
        return "";
    }

    public String getCode() {
        if (recommendations != null && recommendations.has("code")) {
            return recommendations.get("code").asText();
        }
        return "";
    }

    public String getAnalysisDetails() {
        if (recommendations != null && recommendations.has("analysisDetails")) {
            return recommendations.get("analysisDetails").asText();
        }
        return "";
    }
}
