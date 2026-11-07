package com.hangout.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewResponse {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private Double rating;
    private String title;
    private String comment;
    private String photoUrl;
    private Integer helpfulCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // AI Multi-Agent ABSA Scores
    private Integer foodScore;
    private Integer serviceScore;
    private Integer priceScore;
    private Integer ambianceScore;

    // AI Multi-Agent CoT Reasonings
    private String foodThought;
    private String serviceThought;
    private String priceThought;
    private String ambianceThought;

    // Anomaly / Anti-fraud
    private Boolean isAnomaly;
    private String anomalyReason;

    // Corrective RAG recommendations (if client is disappointed)
    private List<RestaurantRecommendation> recommendations = new ArrayList<>();
}
