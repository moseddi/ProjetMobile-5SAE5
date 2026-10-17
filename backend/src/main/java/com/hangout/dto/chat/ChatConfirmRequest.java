package com.hangout.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ChatConfirmRequest(
    @NotNull(message = "L'action est obligatoire") ProposalAction action,
    @Size(max = 64) String reservationId,
    @Size(max = 64) String restaurantId,
    LocalDateTime reservationDateTime,
    Integer partySize,
    @Size(max = 200) String specialRequests
) {}
