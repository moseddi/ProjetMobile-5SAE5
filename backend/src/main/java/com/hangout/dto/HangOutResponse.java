package com.hangout.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HangOutResponse {
    private String id;
    private String creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String restaurantCuisine;
    private String restaurantPriceRange;
    private String title;
    private String description;
    private LocalDateTime plannedDateTime;
    private Integer maxParticipants;
    private List<ParticipantDto> participants;
    private int joinedCount;
    private String status;
    private Boolean aiGenerated;
    private LocalDateTime createdAt;
}
