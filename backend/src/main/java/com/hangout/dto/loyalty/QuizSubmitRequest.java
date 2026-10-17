package com.hangout.dto.loyalty;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    @NotEmpty(message = "At least one answer is required")
    private List<QuizAnswer> answers;

    @Data
    public static class QuizAnswer {
        private String questionId;
        private Integer selectedIndex;
    }
}
