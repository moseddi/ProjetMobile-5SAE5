package com.hangout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventBookingRequest {
    @NotBlank(message = "L'identifiant de l'événement est obligatoire")
    private String eventId;

    @Min(value = 1, message = "Au moins 1 personne")
    @Max(value = 50, message = "50 personnes maximum")
    private Integer partySize;
}
