package com.hangout.service;

import com.hangout.config.ReservationProperties;
import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.AvailabilityResult.Reason;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.util.OpeningHoursParser;
import com.hangout.util.OpeningHoursParser.OpeningHours;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Computes real availability from the reservations stored in MongoDB.
 * A reservation holds its seats from its start time for slotDurationMinutes; two reservations
 * overlap when their start times are less than one slot apart.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {
    
    public static final List<Reservation.ReservationStatus> ACTIVE_STATUSES =
        List.of(Reservation.ReservationStatus.PENDING, Reservation.ReservationStatus.CONFIRMED);
    
    private static final int ALTERNATIVE_STEP_MINUTES = 30;
    private static final int MAX_ALTERNATIVES = 3;
    
    private final RestaurantRepository restaurantRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationProperties properties;
    private final Clock clock;
    
    public AvailabilityResult checkAvailability(String restaurantId, LocalDateTime dateTime, int partySize) {
        return checkAvailability(restaurantId, dateTime, partySize, null);
    }
    
    /**
     * @param excludeReservationId reservation ignored in the seat count (the one being modified), may be null
     */
    public AvailabilityResult checkAvailability(String restaurantId, LocalDateTime dateTime, int partySize,
                                                String excludeReservationId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable"));
        int capacity = capacityOf(restaurant);
        
        if (Boolean.FALSE.equals(restaurant.getIsOpen())) {
            return new AvailabilityResult(false, restaurantId, dateTime, partySize, 0, Reason.RESTAURANT_CLOSED, List.of());
        }
        if (partySize > capacity) {
            return new AvailabilityResult(false, restaurantId, dateTime, partySize, capacity,
                Reason.PARTY_TOO_LARGE_FOR_RESTAURANT, List.of());
        }
        
        List<Reservation> sameDays = activeReservationsAround(restaurantId, dateTime.toLocalDate());
        
        if (!isWithinOpeningHours(restaurant, dateTime.toLocalTime())) {
            return new AvailabilityResult(false, restaurantId, dateTime, partySize, 0, Reason.OUTSIDE_OPENING_HOURS,
                findAlternatives(restaurant, dateTime, partySize, excludeReservationId, sameDays));
        }
        
        int remaining = capacity - seatsTaken(sameDays, dateTime, excludeReservationId);
        if (remaining >= partySize) {
            return new AvailabilityResult(true, restaurantId, dateTime, partySize, remaining, Reason.AVAILABLE, List.of());
        }
        return new AvailabilityResult(false, restaurantId, dateTime, partySize, Math.max(remaining, 0),
            Reason.NOT_ENOUGH_SEATS, findAlternatives(restaurant, dateTime, partySize, excludeReservationId, sameDays));
    }
    
    /**
     * Free start times on one day for the given party size (future times within opening hours only).
     */
    public List<LocalDateTime> availableSlots(String restaurantId, LocalDate day, int partySize, String excludeReservationId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable"));
        if (Boolean.FALSE.equals(restaurant.getIsOpen()) || partySize > capacityOf(restaurant)) {
            return List.of();
        }
        return freeSlots(restaurant, day, partySize, excludeReservationId, activeReservationsAround(restaurantId, day));
    }
    
    private List<LocalDateTime> findAlternatives(Restaurant restaurant, LocalDateTime requested, int partySize,
                                                 String excludeReservationId, List<Reservation> sameDays) {
        List<LocalDateTime> candidates = new ArrayList<>();
        for (LocalDate day : List.of(requested.toLocalDate(), requested.toLocalDate().plusDays(1))) {
            candidates.addAll(freeSlots(restaurant, day, partySize, excludeReservationId, sameDays));
        }
        return candidates.stream()
            .filter(c -> !c.equals(requested))
            .sorted(Comparator.comparing((LocalDateTime c) -> Duration.between(requested, c).abs()))
            .limit(MAX_ALTERNATIVES)
            .sorted()
            .toList();
    }
    
    private List<LocalDateTime> freeSlots(Restaurant restaurant, LocalDate day, int partySize,
                                          String excludeReservationId, List<Reservation> reservations) {
        LocalDateTime now = LocalDateTime.now(clock);
        int capacity = capacityOf(restaurant);
        List<LocalDateTime> slots = new ArrayList<>();
        for (int minutes = 0; minutes < 24 * 60; minutes += ALTERNATIVE_STEP_MINUTES) {
            LocalTime t = LocalTime.MIDNIGHT.plusMinutes(minutes);
            LocalDateTime candidate = day.atTime(t);
            if (candidate.isAfter(now)
                    && isWithinOpeningHours(restaurant, t)
                    && capacity - seatsTaken(reservations, candidate, excludeReservationId) >= partySize) {
                slots.add(candidate);
            }
        }
        return slots;
    }
    
    // Loads the requested day and the next one (+ one slot on each side) so alternatives need no extra query
    private List<Reservation> activeReservationsAround(String restaurantId, LocalDate day) {
        Duration slot = slotDuration();
        return reservationRepository.findByRestaurantIdAndStatusInAndReservationDateTimeBetween(
            restaurantId, ACTIVE_STATUSES,
            day.atStartOfDay().minus(slot),
            day.plusDays(2).atStartOfDay().plus(slot));
    }
    
    int seatsTaken(List<Reservation> reservations, LocalDateTime dateTime, String excludeReservationId) {
        Duration slot = slotDuration();
        return reservations.stream()
            .filter(r -> !Objects.equals(r.getId(), excludeReservationId))
            .filter(r -> ACTIVE_STATUSES.contains(r.getStatus()))
            .filter(r -> Duration.between(r.getReservationDateTime(), dateTime).abs().compareTo(slot) < 0)
            .mapToInt(r -> r.getPartySize() == null ? 0 : r.getPartySize())
            .sum();
    }
    
    public boolean isWithinOpeningHours(Restaurant restaurant, LocalTime time) {
        Optional<OpeningHours> hours = OpeningHoursParser.parse(restaurant.getOpeningHours());
        if (hours.isEmpty()) {
            return true; // unknown format: do not block the reservation on it
        }
        LocalTime open = hours.get().open();
        LocalTime close = hours.get().close();
        if (!close.isAfter(open)) {
            close = LocalTime.of(23, 59); // closes after midnight: last seating computed from end of day
        }
        LocalTime lastSeating = close.minusMinutes(properties.getLastSeatingMinutesBeforeClose());
        return !time.isBefore(open) && !time.isAfter(lastSeating);
    }
    
    public int capacityOf(Restaurant restaurant) {
        Integer capacity = restaurant.getSeatingCapacity();
        return capacity != null && capacity > 0 ? capacity : properties.getDefaultCapacity();
    }
    
    private Duration slotDuration() {
        return Duration.ofMinutes(properties.getSlotDurationMinutes());
    }
}
