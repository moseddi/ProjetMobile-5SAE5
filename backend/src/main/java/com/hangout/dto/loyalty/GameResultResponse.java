package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

/** Generic result returned after playing any loyalty game. */
@Data
public class GameResultResponse {
    private String gameType;
    private Integer score;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Integer pointsEarned;
    private Integer newBalance;
    private String outcome;
    private String message;
    private List<QuizExplanation> explanations;

    @Data
    public static class QuizExplanation {
        private String questionId;
        private boolean correct;
        private Integer correctAnswerIndex;
        private String explanation;
    }
}
