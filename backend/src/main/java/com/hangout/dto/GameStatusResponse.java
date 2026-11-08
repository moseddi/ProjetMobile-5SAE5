package com.hangout.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** Where a client stands with one daily game. */
@Data
@AllArgsConstructor
public class GameStatusResponse {
    private String gameType;
    private String title;
    private String description;
    private boolean playedToday;
    /** True when another game was already played today (one game per day). */
    private boolean locked;
    private Integer lastPoints;
    private String lastRestaurantName;
    /** When the game becomes playable again (midnight), only set when played today. */
    private LocalDateTime availableAgainAt;
}
