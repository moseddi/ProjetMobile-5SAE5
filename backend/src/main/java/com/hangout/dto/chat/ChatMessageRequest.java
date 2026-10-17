package com.hangout.dto.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatMessageRequest(
    @NotBlank(message = "Le message est vide")
    @Size(max = 500, message = "Le message ne doit pas dépasser 500 caractères")
    String message,
    
    @Valid
    @Size(max = 30)
    List<ChatTurn> history,
    
    /** Restaurant the user came from (Reserve a Table button), optional. */
    @Size(max = 64)
    String restaurantId,

    /** Event the user came from (Reserve an Event button), optional. */
    @Size(max = 64)
    String eventId
) {
    public List<ChatTurn> historyOrEmpty() {
        return history == null ? List.of() : history;
    }
}
