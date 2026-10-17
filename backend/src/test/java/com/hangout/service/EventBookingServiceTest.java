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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the authoritative event-booking write path: capacity, duplicates, ownership
 * and past-event checks must hold whether the booking comes from the REST API or the chatbot.
 */
@DisplayName("EventBookingService - capacity, ownership and duplicate rules (unit)")
class EventBookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 15, 12, 0).atZone(ZONE).toInstant(), ZONE);

    private EventBookingRepository eventBookingRepository;
    private EventRepository eventRepository;
    private EventBookingService service;

    @BeforeEach
    void setUp() {
        eventBookingRepository = mock(EventBookingRepository.class);
        eventRepository = mock(EventRepository.class);
        service = new EventBookingService(eventBookingRepository, eventRepository, CLOCK);

        when(eventBookingRepository.save(any(EventBooking.class))).thenAnswer(inv -> {
            EventBooking b = inv.getArgument(0);
            if (b.getId() == null) b.setId("booking-1");
            return b;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Event futureEvent(int availableSpots, int bookedSpots) {
        Event event = new Event();
        event.setId("e1");
        event.setTitle("Dégustation de vin");
        event.setEventDate(LocalDateTime.now(CLOCK).plusDays(2));
        event.setAvailableSpots(availableSpots);
        event.setBookedSpots(bookedSpots);
        return event;
    }

    @Test
    @DisplayName("books successfully when enough spots remain and increments bookedSpots")
    void bookEvent_success_incrementsBookedSpots() {
        Event event = futureEvent(20, 5);
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));
        when(eventBookingRepository.findByEventId("e1")).thenReturn(List.of());

        EventBooking saved = service.bookEvent("u1", "e1", 3);

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getPartySize()).isEqualTo(3);
        verify(eventRepository).save(argThat(e -> e.getBookedSpots() == 8));
    }

    @Test
    @DisplayName("rejects a booking when the event is already in the past")
    void bookEvent_pastEvent_throwsBadRequest() {
        Event event = futureEvent(20, 0);
        event.setEventDate(LocalDateTime.now(CLOCK).minusDays(1));
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.bookEvent("u1", "e1", 2))
            .isInstanceOf(BadRequestException.class);
        verify(eventBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a booking when not enough spots remain")
    void bookEvent_notEnoughSpots_throwsConflict() {
        Event event = futureEvent(10, 9);
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));
        when(eventBookingRepository.findByEventId("e1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.bookEvent("u1", "e1", 5))
            .isInstanceOf(ConflictException.class);
        verify(eventBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a second active booking by the same user for the same event")
    void bookEvent_duplicateActiveBooking_throwsConflict() {
        Event event = futureEvent(20, 2);
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));
        EventBooking existing = new EventBooking();
        existing.setUserId("u1");
        existing.setEventId("e1");
        existing.setStatus(BookingStatus.PENDING);
        when(eventBookingRepository.findByEventId("e1")).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.bookEvent("u1", "e1", 2))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("allows booking again after a previous booking was cancelled")
    void bookEvent_afterCancelledBooking_succeeds() {
        Event event = futureEvent(20, 2);
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));
        EventBooking cancelled = new EventBooking();
        cancelled.setUserId("u1");
        cancelled.setEventId("e1");
        cancelled.setStatus(BookingStatus.CANCELLED);
        when(eventBookingRepository.findByEventId("e1")).thenReturn(List.of(cancelled));

        EventBooking saved = service.bookEvent("u1", "e1", 2);

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("rejects booking for an unknown event")
    void bookEvent_unknownEvent_throwsNotFound() {
        when(eventRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookEvent("u1", "ghost", 2))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelBooking restores the event's spots and marks the booking cancelled")
    void cancelBooking_restoresSpotsAndCancels() {
        EventBooking booking = new EventBooking();
        booking.setId("b1");
        booking.setUserId("u1");
        booking.setEventId("e1");
        booking.setPartySize(3);
        booking.setStatus(BookingStatus.PENDING);
        when(eventBookingRepository.findById("b1")).thenReturn(Optional.of(booking));
        Event event = futureEvent(20, 8);
        when(eventRepository.findById("e1")).thenReturn(Optional.of(event));

        EventBooking cancelled = service.cancelBooking("b1", "u1");

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(eventRepository).save(argThat(e -> e.getBookedSpots() == 5));
    }

    @Test
    @DisplayName("cancelBooking rejects a request from someone other than the owner")
    void cancelBooking_notOwner_throwsForbidden() {
        EventBooking booking = new EventBooking();
        booking.setId("b1");
        booking.setUserId("owner");
        booking.setStatus(BookingStatus.PENDING);
        when(eventBookingRepository.findById("b1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking("b1", "someone-else"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("cancelBooking rejects a booking that is already cancelled")
    void cancelBooking_alreadyCancelled_throwsBadRequest() {
        EventBooking booking = new EventBooking();
        booking.setId("b1");
        booking.setUserId("u1");
        booking.setStatus(BookingStatus.CANCELLED);
        when(eventBookingRepository.findById("b1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking("b1", "u1"))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("remainingSpots never goes negative when overbooked")
    void remainingSpots_neverNegative() {
        Event event = futureEvent(10, 15);
        assertThat(service.remainingSpots(event)).isZero();
    }
}
