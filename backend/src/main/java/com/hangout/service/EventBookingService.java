package com.hangout.service;

import com.hangout.exception.BadRequestException;
import com.hangout.exception.ConflictException;
import com.hangout.exception.ForbiddenException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.model.EventBooking;
import com.hangout.model.EventBooking.BookingStatus;
import com.hangout.repository.EventBookingRepository;
import com.hangout.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Single entry point for every event-booking write: the chatbot and the REST API both go
 * through it, so capacity, ownership and duplicate rules always apply the same way.
 */
@Service
@RequiredArgsConstructor
public class EventBookingService {

    public static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final EventBookingRepository eventBookingRepository;
    private final EventRepository eventRepository;
    private final Clock clock;

    public List<EventBooking> getUserBookings(String userId) {
        return eventBookingRepository.findByUserId(userId);
    }

    public EventBooking getBookingForUser(String id, String requesterUserId) {
        EventBooking booking = eventBookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Réservation d'événement introuvable"));
        checkOwner(booking, requesterUserId);
        return booking;
    }

    /** Remaining bookable spots for an event, never negative. */
    public int remainingSpots(Event event) {
        int capacity = event.getAvailableSpots() != null ? event.getAvailableSpots() : 0;
        int booked = event.getBookedSpots() != null ? event.getBookedSpots() : 0;
        return Math.max(0, capacity - booked);
    }

    @Transactional
    public EventBooking bookEvent(String userId, String eventId, int partySize) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable"));
        if (event.getEventDate() != null && !event.getEventDate().isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Cet événement est déjà passé");
        }
        if (partySize < 1) {
            throw new BadRequestException("Le nombre de personnes doit être d'au moins 1");
        }
        if (hasActiveBooking(userId, eventId)) {
            throw new ConflictException("Vous avez déjà une réservation pour cet événement");
        }
        if (remainingSpots(event) < partySize) {
            throw new ConflictException("Il ne reste pas assez de places pour cet événement");
        }

        EventBooking booking = new EventBooking();
        booking.setUserId(userId);
        booking.setEventId(eventId);
        booking.setPartySize(partySize);
        booking.setStatus(BookingStatus.PENDING);
        EventBooking saved = eventBookingRepository.save(booking);

        event.setBookedSpots((event.getBookedSpots() != null ? event.getBookedSpots() : 0) + partySize);
        eventRepository.save(event);

        return saved;
    }

    @Transactional
    public EventBooking cancelBooking(String id, String requesterUserId) {
        EventBooking booking = getBookingForUser(id, requesterUserId);
        if (!ACTIVE_BOOKING_STATUSES.contains(booking.getStatus())) {
            throw new BadRequestException("Cette réservation n'est plus active");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        EventBooking saved = eventBookingRepository.save(booking);

        eventRepository.findById(booking.getEventId()).ifPresent(event -> {
            int booked = event.getBookedSpots() != null ? event.getBookedSpots() : 0;
            event.setBookedSpots(Math.max(0, booked - booking.getPartySize()));
            eventRepository.save(event);
        });

        return saved;
    }

    private boolean hasActiveBooking(String userId, String eventId) {
        return eventBookingRepository.findByEventId(eventId).stream()
            .anyMatch(b -> userId.equals(b.getUserId()) && ACTIVE_BOOKING_STATUSES.contains(b.getStatus()));
    }

    private void checkOwner(EventBooking booking, String requesterUserId) {
        if (requesterUserId == null || !requesterUserId.equals(booking.getUserId())) {
            throw new ForbiddenException("Cette réservation ne vous appartient pas");
        }
    }
}
