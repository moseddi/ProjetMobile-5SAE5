package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

@Data
public class RestaurantRecommendationResponse {
    private int points;
    private String goal;
    private String message;
    private List<RestaurantRecommendationCard> recommendations;
    private boolean aiGenerated;
    private String notice;
}
