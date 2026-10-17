package com.hangout.dto.loyalty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemorySubmitRequest {
    @NotBlank(message = "Session id is required")
    private String sessionId;

    @Min(value = 1, message = "Moves must be at least 1")
    private Integer moves;

    @Min(value = 1, message = "Duration must be positive")
    private Integer durationMs;
}
