package com.hangout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResult {
    // Multi-criteria ABSA Scores (1 to 10)
    private Integer foodScore;
    private Integer serviceScore;
    private Integer priceScore;
    private Integer ambianceScore;

    // Chain-of-Thought (CoT) Reasonings from the 4 Agents
    private String foodThought;
    private String serviceThought;
    private String priceThought;
    private String ambianceThought;

    // Real-time Anomaly / Anti-fraud Detection
    private Boolean isAnomaly;
    private String anomalyReason;

    // Corrective Recommendations (RAG)
    @Builder.Default
    private List<RestaurantRecommendation> recommendations = new ArrayList<>();
}
