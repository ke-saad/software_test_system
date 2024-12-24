package com.emsi.results_recommendations_service.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Summary {
    private int totalTests;
    private int passed;
    private int failed;
    private int skipped;

}