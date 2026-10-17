package com.hangout.dto.chat;

import com.hangout.chatbot.ChatIntent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every sentence in "reply" is written by the backend from real data; the model only classifies and extracts.
 */
public record ChatResponse(
    String reply,
    ChatIntent intent,
    Type type,
    ReservationProposal proposal,
    List<LocalDateTime> suggestedSlots,
    List<ReservationSummary> reservations,
    String restaurantId,
    EventBookingProposal eventProposal,
    List<EventBookingSummary> eventBookings,
    String eventId
) {
    public enum Type {
        ANSWER,
        QUESTION,
        PROPOSAL,
        CONFIRMED,
        OUT_OF_SCOPE
    }

    public static ChatResponse of(String reply, ChatIntent intent, Type type) {
        return new ChatResponse(reply, intent, type, null, List.of(), List.of(), null, null, List.of(), null);
    }

    public ChatResponse withProposal(ReservationProposal value) {
        return new ChatResponse(reply, intent, type, value, suggestedSlots, reservations, restaurantId,
            eventProposal, eventBookings, eventId);
    }

    public ChatResponse withSlots(List<LocalDateTime> value) {
        return new ChatResponse(reply, intent, type, proposal, value, reservations, restaurantId,
            eventProposal, eventBookings, eventId);
    }

    public ChatResponse withReservations(List<ReservationSummary> value) {
        return new ChatResponse(reply, intent, type, proposal, suggestedSlots, value, restaurantId,
            eventProposal, eventBookings, eventId);
    }

    public ChatResponse withRestaurant(String value) {
        return new ChatResponse(reply, intent, type, proposal, suggestedSlots, reservations, value,
            eventProposal, eventBookings, eventId);
    }

    public ChatResponse withEventProposal(EventBookingProposal value) {
        return new ChatResponse(reply, intent, type, proposal, suggestedSlots, reservations, restaurantId,
            value, eventBookings, eventId);
    }

    public ChatResponse withEventBookings(List<EventBookingSummary> value) {
        return new ChatResponse(reply, intent, type, proposal, suggestedSlots, reservations, restaurantId,
            eventProposal, value, eventId);
    }

    public ChatResponse withEvent(String value) {
        return new ChatResponse(reply, intent, type, proposal, suggestedSlots, reservations, restaurantId,
            eventProposal, eventBookings, value);
    }
}
