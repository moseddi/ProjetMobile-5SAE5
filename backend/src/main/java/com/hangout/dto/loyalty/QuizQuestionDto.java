package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

/** Public view of a quiz question — never exposes the correct answer index. */
@Data
public class QuizQuestionDto {
    private String id;
    private String question;
    private List<String> options;
    private String category;
    private String difficulty;
}
