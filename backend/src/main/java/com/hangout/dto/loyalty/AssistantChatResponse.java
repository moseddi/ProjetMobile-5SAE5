package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatResponse {
    private String intent;
    private String message;
    private List<Recommendation> recommendations;
    private String followUpQuestion;
    private boolean aiGenerated;
    private String notice;

    @Data
    public static class Recommendation {
        private String type; // GAME | REWARD
        private String id;
        private String title;
        private String reason;
        private String action; // VIEW_GAME | START_GAME | VIEW_REWARD | REDEEM_REWARD
    }
}
