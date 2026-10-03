package com.hangout.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GamePlayRequest {
    @NotNull(message = "Game outcome is required")
    private String outcome; // WIN, LOSE, DRAW
    
    private Integer score;
}
