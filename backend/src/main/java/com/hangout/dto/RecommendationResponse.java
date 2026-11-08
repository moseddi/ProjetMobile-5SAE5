package com.hangout.dto;

import lombok.Data;

import java.util.List;

/** Personalised suggestions for a client: where to spend points, where to go next. */
@Data
public class RecommendationResponse {
    /** GEMINI when the language model wrote them, FALLBACK for the built-in rules. */
    private String source;
    private String summary;
    /** Why the fallback was used, when it was (never shown as an error). */
    private String notice;
    private List<Suggestion> suggestions;

    @Data
    public static class Suggestion {
        /** EXCHANGE (spend points now), SAVE (almost enough), VISIT (try a place), PLAY (daily game). */
        private String type;
        private String title;
        private String reason;
        private String restaurantId;
        private String restaurantName;
        private String restaurantImage;
        private String rewardType;
        private String rewardTitle;
        private int cost;
        private int restaurantPoints;
        private boolean canRedeem;
        private int missingPoints;
    }
}
