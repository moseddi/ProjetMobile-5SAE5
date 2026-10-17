package com.hangout.chatbot;

import com.hangout.model.Event;
import com.hangout.model.Restaurant;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Model extraction after backend validation. Invalid values are set to null and explained in "problems",
 * which the assistant turns into a targeted question.
 *
 * @param restaurantNamed true when the user named the restaurant in the conversation (not only page context)
 * @param eventNamed true when the user named the event in the conversation
 */
public record ValidatedRequest(
    ChatIntent intent,
    Restaurant restaurant,
    boolean restaurantNamed,
    LocalDate date,
    LocalTime time,
    Integer partySize,
    Integer reservationNumber,
    LocalDate targetDate,
    String specialRequests,
    InfoTopic infoTopic,
    Event event,
    boolean eventNamed,
    List<String> problems
) {
    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
