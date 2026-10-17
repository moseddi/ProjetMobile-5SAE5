package com.hangout.service;

import com.hangout.config.ReservationProperties;
import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.AvailabilityResult.Reason;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.Reservation.ReservationStatus;
import com.hangout.model.Restaurant;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    
    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    // "Now" is fixed to 15/10/2026 12:00 so dates in tests are always in the future
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 15, 12, 0).atZone(ZONE).toInstant(), ZONE);
    private static final LocalDateTime TOMORROW_20H = LocalDateTime.of(2026, 10, 16, 20, 0);
    
    @Mock RestaurantRepository restaurantRepository;
    @Mock ReservationRepository reservationRepository;
    
    private AvailabilityService service;
    private Restaurant restaurant;
    private final List<Reservation> stored = new ArrayList<>();
    
    @BeforeEach
    void setUp() {
        service = new AvailabilityService(restaurantRepository, reservationRepository, new ReservationProperties(), CLOCK);
        restaurant = new Restaurant();
        restaurant.setId("r1");
        restaurant.setOpeningHours("11:00 AM - 10:00 PM");
        restaurant.setSeatingCapacity(10);
        restaurant.setIsOpen(true);
        org.mockito.Mockito.lenient().when(restaurantRepository.findById("r1")).thenReturn(Optional.of(restaurant));
        org.mockito.Mockito.lenient().when(reservationRepository.findByRestaurantIdAndStatusInAndReservationDateTimeBetween(
                eq("r1"), anyCollection(), any(), any()))
            .thenAnswer(inv -> stored);
    }
    
    private Reservation existing(String id, LocalDateTime at, int party, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId("other");
        r.setRestaurantId("r1");
        r.setReservationDateTime(at);
        r.setPartySize(party);
        r.setStatus(status);
        stored.add(r);
        return r;
    }
    
    @Test
    void availableWhenNoReservation() {
        AvailabilityResult result = service.checkAvailability("r1", TOMORROW_20H, 2);
        assertThat(result.available()).isTrue();
        assertThat(result.reason()).isEqualTo(Reason.AVAILABLE);
        assertThat(result.remainingSeats()).isEqualTo(10);
    }
    
    @Test
    void notEnoughSeatsWhenOverlappingReservationsFillTheRoom() {
        existing("a", TOMORROW_20H.minusMinutes(60), 6, ReservationStatus.CONFIRMED);
        existing("b", TOMORROW_20H.plusMinutes(30), 3, ReservationStatus.PENDING);
        
        AvailabilityResult result = service.checkAvailability("r1", TOMORROW_20H, 2);
        
        assertThat(result.available()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.NOT_ENOUGH_SEATS);
        assertThat(result.remainingSeats()).isEqualTo(1);
        assertThat(result.alternatives()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        // every proposed alternative must really be available
        for (LocalDateTime alt : result.alternatives()) {
            assertThat(service.checkAvailability("r1", alt, 2).available()).as(alt.toString()).isTrue();
        }
    }
    
    @Test
    void reservationExactlyOneSlotEarlierDoesNotOverlap() {
        existing("a", TOMORROW_20H.minusHours(2), 10, ReservationStatus.CONFIRMED);
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 10).available()).isTrue();
    }
    
    @Test
    void cancelledAndCompletedReservationsDoNotTakeSeats() {
        existing("a", TOMORROW_20H, 10, ReservationStatus.CANCELLED);
        existing("b", TOMORROW_20H, 10, ReservationStatus.COMPLETED);
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 10).available()).isTrue();
    }
    
    @Test
    void excludedReservationIsIgnoredWhenModifyingIt() {
        existing("mine", TOMORROW_20H, 8, ReservationStatus.CONFIRMED);
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 9).available()).isFalse();
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 9, "mine").available()).isTrue();
    }
    
    @Test
    void refusesOutsideOpeningHoursAndProposesAlternatives() {
        AvailabilityResult result = service.checkAvailability("r1", TOMORROW_20H.withHour(23), 2);
        assertThat(result.available()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.OUTSIDE_OPENING_HOURS);
        assertThat(result.alternatives()).isNotEmpty();
    }
    
    @Test
    void lastSeatingIsOneHourBeforeClosing() {
        assertThat(service.checkAvailability("r1", TOMORROW_20H.withHour(21), 2).available()).isTrue();
        assertThat(service.checkAvailability("r1", TOMORROW_20H.withHour(21).withMinute(30), 2).available()).isFalse();
        assertThat(service.checkAvailability("r1", TOMORROW_20H.withHour(10).withMinute(30), 2).available()).isFalse();
    }
    
    @Test
    void refusesPartyLargerThanRestaurantCapacity() {
        AvailabilityResult result = service.checkAvailability("r1", TOMORROW_20H, 11);
        assertThat(result.reason()).isEqualTo(Reason.PARTY_TOO_LARGE_FOR_RESTAURANT);
        assertThat(result.alternatives()).isEmpty();
    }
    
    @Test
    void refusesWhenRestaurantIsClosed() {
        restaurant.setIsOpen(false);
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 2).reason()).isEqualTo(Reason.RESTAURANT_CLOSED);
    }
    
    @Test
    void usesDefaultCapacityWhenRestaurantHasNone() {
        restaurant.setSeatingCapacity(null);
        assertThat(service.checkAvailability("r1", TOMORROW_20H, 20).remainingSeats()).isEqualTo(40);
    }
    
    @Test
    void alternativesAreNeverInThePast() {
        existing("a", LocalDateTime.of(2026, 10, 15, 13, 0), 10, ReservationStatus.CONFIRMED);
        AvailabilityResult result = service.checkAvailability("r1", LocalDateTime.of(2026, 10, 15, 13, 0), 2);
        assertThat(result.alternatives()).allMatch(alt -> alt.isAfter(LocalDateTime.now(CLOCK)));
    }
    
    @Test
    void unknownRestaurantThrowsNotFound() {
        when(restaurantRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkAvailability("nope", TOMORROW_20H, 2))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
