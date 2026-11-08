package com.hangout.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** The client only says where it plays; the server decides the reward. */
@Data
public class GamePlayRequest {
    @NotBlank(message = "Choose the restaurant you are playing for")
    private String restaurantId;
}
