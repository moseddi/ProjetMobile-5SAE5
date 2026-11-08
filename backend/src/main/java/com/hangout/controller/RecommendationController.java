package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.RecommendationResponse;
import com.hangout.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<RecommendationResponse> recommend() {
        return ResponseEntity.ok(recommendationService.recommend(SecurityUtils.getCurrentUserId()));
    }
}
