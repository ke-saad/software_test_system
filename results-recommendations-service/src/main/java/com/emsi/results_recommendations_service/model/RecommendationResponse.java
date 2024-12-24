package com.emsi.results_recommendations_service.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {
    private String testRunId;
    private Summary summary;
    private List<Recommendation> recommendations;
}