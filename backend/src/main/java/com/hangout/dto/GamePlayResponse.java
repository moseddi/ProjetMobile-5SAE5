package com.hangout.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GamePlayResponse {
    private String id;
    private String gameType;
    private String restaurantId;
    private String restaurantName;
    private int pointsEarned;
    /** Total points after this play, all restaurants together. */
    private int totalPoints;
    /** Points the client now has at this restaurant. */
    private int restaurantPoints;
    private LocalDateTime playedAt;
}
