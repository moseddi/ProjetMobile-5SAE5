package com.hangout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRecommendation {
    private String id;
    private String name;
    private String cuisine;
    private String description;
    private String image;
    private Double rating;
    private String address;
    private String priceRange;
    private String recommendationReason; // e.g. "Excellence culinaire certifiée (Score: 9/10)"
    private String strongCriterion;       // "Cuisine", "Service", "Prix", or "Ambiance"
}
