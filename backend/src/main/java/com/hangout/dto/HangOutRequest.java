package com.hangout.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HangOutRequest {
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Planned date and time is required")
    @Future(message = "Planned date must be in the future")
    private LocalDateTime plannedDateTime;

    @NotNull(message = "Max participants is required")
    @Min(value = 2, message = "A hangout needs at least 2 participants")
    private Integer maxParticipants;

    // GPS Coordinates for the restaurant
    private Double restaurantLatitude;
    private Double restaurantLongitude;

    // Meeting point details
    private String meetingPointName;
    private Double meetingPointLatitude;
    private Double meetingPointLongitude;
}
