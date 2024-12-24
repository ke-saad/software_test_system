package com.emsi.results_recommendations_service.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    private String type;
    private String message;
    private Location location;
    public Recommendation(String type, String message){
        this.type = type;
        this.message = message;
    }
}