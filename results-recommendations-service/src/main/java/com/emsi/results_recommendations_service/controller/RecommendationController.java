package com.emsi.results_recommendations_service.controller;

import com.emsi.results_recommendations_service.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendation")
public class RecommendationController {

    @Autowired
    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }


}