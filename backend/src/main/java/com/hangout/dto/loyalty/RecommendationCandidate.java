package com.hangout.dto.loyalty;

import java.time.LocalDateTime;

/**
 * A restaurant + reward pairing already filtered and ranked by trusted backend logic,
 * handed to the LLM only to be explained in natural language — never to be invented or re-ranked.
 */
public record RecommendationCandidate(
    String restaurantId,
    String restaurantName,
    String cuisine,
    String priceRange,
    Double rating,
    String rewardId,
    String rewardName,
    String rewardCategory,
    int pointsCost,
    boolean affordable,
    int pointsMissing,
    LocalDateTime expiryDate,
    String termsAndConditions,
    boolean matchesPreferredCuisine
) {
    public String key() {
        return restaurantId + "|" + rewardId;
    }
}
