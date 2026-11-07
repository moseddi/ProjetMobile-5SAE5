package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    
    private String userId;
    private String restaurantId;
    private Double rating; // 1.0 to 5.0
    private String title;
    private String comment;
    private String photoUrl;
    private Integer helpfulCount = 0;
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    // AI Multi-Agent ABSA Scores (1 to 10)
    private Integer foodScore;
    private Integer serviceScore;
    private Integer priceScore;
    private Integer ambianceScore;

    // AI Multi-Agent Chain-of-Thought (CoT) Reasonings
    private String foodThought;
    private String serviceThought;
    private String priceThought;
    private String ambianceThought;

    // Anti-Fraud / Real-time Anomaly Detection
    private Boolean isAnomaly = false;
    private String anomalyReason;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum ReviewStatus {
        PENDING, PUBLISHED, REJECTED, DELETED
    }
}
