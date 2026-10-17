package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/** Per-candidate explanation text, keyed by {@link RecommendationCandidate#key()}. */
@Data
public class RecommendationExplanations {
    private Map<String, String> reasonsByKey = new HashMap<>();
    private String overallMessage;
    private boolean aiGenerated;
    private String notice;

    public String reasonFor(RecommendationCandidate candidate) {
        return reasonsByKey.get(candidate.key());
    }
}
