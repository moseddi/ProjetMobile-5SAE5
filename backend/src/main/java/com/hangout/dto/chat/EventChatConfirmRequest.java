package com.hangout.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventChatConfirmRequest(
    @NotNull(message = "L'action est obligatoire") EventProposalAction action,
    @Size(max = 64) String bookingId,
    @Size(max = 64) String eventId,
    Integer partySize
) {}
