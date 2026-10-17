package com.hangout.dto.loyalty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCatalogEntry {
    private String id;
    private String name;
    private String description;
    private String category;
    private String difficulty;
    private int estimatedDurationMinutes;
    private String icon;
    private String rules;
    private String pointsReward; // range or formula description, e.g. "10-50"
    private int dailyLimit;
    private int attemptsUsedToday;
    private boolean availableToday;
}
