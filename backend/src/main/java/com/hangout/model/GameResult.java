package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "game_results")
public class GameResult {
    @Id
    private String id;
    
    private String userId;
    private String restaurantId;
    private GameType gameType;
    private Integer pointsEarned;
    private GameOutcome outcome;
    
    @CreatedDate
    private LocalDateTime playedAt;
    
    public enum GameType {
        CARD_MEMORY, LUCKY_WHEEL, SCRATCH_CARD, LUCKY_DICE,
        // Legacy values kept for backward-compatibility with existing data; not used by new code.
        DAILY_WHEEL, QUIZ
    }
    
    public enum GameOutcome {
        WIN, LOSE, DRAW
    }
}
