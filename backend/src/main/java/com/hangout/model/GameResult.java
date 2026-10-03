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
    private GameType gameType;
    private Integer pointsEarned;
    private GameOutcome outcome;
    
    @CreatedDate
    private LocalDateTime playedAt;
    
    public enum GameType {
        CARD_MEMORY, LUCKY_WHEEL
    }
    
    public enum GameOutcome {
        WIN, LOSE, DRAW
    }
}
