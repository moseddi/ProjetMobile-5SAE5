package com.hangout.dto;

import com.hangout.model.Event;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String image;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Double price;

    @NotNull(message = "Available spots is required")
    @Min(value = 1, message = "Must have at least 1 spot")
    private Integer availableSpots;

    @NotBlank(message = "Event type is required")
    @Size(max = 40, message = "Event type must be at most 40 characters")
    private String category;
}
