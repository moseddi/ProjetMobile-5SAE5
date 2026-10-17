package com.hangout.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HangOutRequest {

    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title cannot exceed 120 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Planned date and time is required")
    @Future(message = "Planned date and time must be in the future")
    private LocalDateTime plannedDateTime;

    @NotNull(message = "Maximum participants is required")
    @Min(value = 2, message = "A hangout needs at least 2 participants")
    @Max(value = 50, message = "Maximum participants cannot exceed 50")
    private Integer maxParticipants;

    private Boolean aiGenerated = false;
}
