package com.hangout.dto.chat;

import java.time.LocalDateTime;

/**
 * Action checked against the database and waiting for the user's explicit confirmation.
 * Nothing is written until the user confirms, and the confirmation is validated again.
 */
public record EventBookingProposal(
    EventProposalAction action,
    String bookingId,
    String eventId,
    String eventTitle,
    LocalDateTime eventDate,
    Integer partySize
) {}
