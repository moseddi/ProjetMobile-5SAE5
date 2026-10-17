package com.hangout.dto.loyalty;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RestaurantRecommendationCard {
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String cuisine;
    private String priceRange;
    private Double rating;

    private String rewardId;
    private String rewardName;
    private String rewardCategory;
    private Integer pointsCost;
    private boolean affordable;
    private Integer pointsMissing;
    private Integer pointsRemainingAfterRedemption;
    private LocalDateTime rewardExpiryDate;
    private String rewardTerms;

    private String reason;
    private String action; // VIEW_RESTAURANT | VIEW_REWARD
}
