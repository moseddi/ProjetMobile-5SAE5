package com.hangout.dto.loyalty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private String id;
    private String title;
    private String description;
    private Integer pointsReward;
    private int progress;
    private int target;
    private boolean completed;
}
