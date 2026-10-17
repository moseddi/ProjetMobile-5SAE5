package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "quiz_questions")
public class QuizQuestion {
    @Id
    private String id;

    private String question;
    private List<String> options;
    private Integer correctAnswerIndex;
    private String explanation;
    private String category;
    private Difficulty difficulty;
    private Boolean active = true;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}
