package com.hangout.dto.chat;

import java.time.LocalDateTime;

/**
 * Action checked against the database and waiting for the user's explicit confirmation.
 * Nothing is written until the user confirms, and the confirmation is validated again.
 */
public record ReservationProposal(
    ProposalAction action,
    String reservationId,
    String restaurantId,
    String restaurantName,
    LocalDateTime reservationDateTime,
    Integer partySize,
    String specialRequests
) {}
