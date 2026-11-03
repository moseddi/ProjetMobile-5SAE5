package com.hangout.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EventRegistrationRequest {
    @NotBlank(message = "Your name is required")
    @Size(max = 80, message = "Name must be at most 80 characters")
    private String contactName;

    @NotBlank(message = "Your phone number is required")
    @Pattern(regexp = "^[+]?[0-9 ()-]{6,20}$", message = "Enter a valid phone number")
    private String contactPhone;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "At least 1 person")
    @Max(value = 20, message = "At most 20 people")
    private Integer partySize;
}
