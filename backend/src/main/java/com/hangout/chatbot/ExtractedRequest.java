package com.hangout.chatbot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw fields extracted by the model. Nothing here is trusted: ChatIntentValidator checks every value.
 * For MODIFY, date/time/partySize are the NEW values and targetDate identifies the existing reservation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedRequest(
    String intent,
    String restaurantName,
    String date,
    String time,
    Integer partySize,
    Integer reservationNumber,
    String targetDate,
    String specialRequests,
    String infoTopic,
    String eventTitle
) {
    public ChatIntent chatIntent() {
        return ChatIntent.fromModel(intent);
    }
}
