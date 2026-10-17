package com.hangout.chatbot;

import com.hangout.model.EventBooking.BookingStatus;
import com.hangout.model.Reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** French formatting of values that come from the database, used to build every assistant sentence. */
public final class ChatFormatter {
    
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE d MMM 'à' HH'h'mm", Locale.FRENCH);
    
    private ChatFormatter() {}
    
    public static String date(LocalDate date) {
        return date.format(DATE);
    }
    
    public static String time(LocalTime time) {
        return time.format(TIME);
    }
    
    public static String dateTime(LocalDateTime dateTime) {
        return date(dateTime.toLocalDate()) + " à " + time(dateTime.toLocalTime());
    }
    
    /** Times only when all slots are on the same day, otherwise short date + time. */
    public static String slots(List<LocalDateTime> slots) {
        boolean sameDay = slots.stream().map(LocalDateTime::toLocalDate).distinct().count() <= 1;
        return slots.stream()
            .map(s -> sameDay ? time(s.toLocalTime()) : s.format(SHORT))
            .collect(Collectors.joining(", "));
    }
    
    public static String persons(int partySize) {
        return partySize + (partySize > 1 ? " personnes" : " personne");
    }
    
    public static String status(ReservationStatus status) {
        return switch (status) {
            case PENDING -> "en attente de confirmation par le restaurant";
            case CONFIRMED -> "confirmée";
            case CANCELLED -> "annulée";
            case COMPLETED -> "terminée";
        };
    }

    public static String eventBookingStatus(BookingStatus status) {
        return switch (status) {
            case PENDING -> "en attente de confirmation";
            case CONFIRMED -> "confirmée";
            case CANCELLED -> "annulée";
        };
    }
}
