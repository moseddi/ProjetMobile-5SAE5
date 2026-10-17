package com.hangout.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AiHangoutRequest {

    @NotBlank(message = "Please describe your hangout idea")
    @Size(max = 1000, message = "Description is too long")
    private String prompt;

    @Min(value = 2, message = "Party size must be at least 2")
    @Max(value = 50, message = "Party size cannot exceed 50")
    private Integer partySize;

    private String budget;
}
