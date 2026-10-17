package com.hangout.service;

import com.hangout.config.ReservationProperties;
import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.AvailabilityResult.Reason;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ConflictException;
import com.hangout.exception.ForbiddenException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.Reservation.ReservationStatus;
import com.hangout.model.User;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {
    
    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 15, 12, 0).atZone(ZONE).toInstant(), ZONE);
    private static final LocalDateTime TOMORROW_20H = LocalDateTime.of(2026, 10, 16, 20, 0);
    
    @Mock ReservationRepository reservationRepository;
    @Mock UserRepository userRepository;
    @Mock AvailabilityService availabilityService;
    
    private ReservationService service;
    private User user;
    private final List<Reservation> stored = new ArrayList<>();
    
    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepository, userRepository, availabilityService,
            new ReservationProperties(), CLOCK);
        user = new User();
        user.setId("u1");
        user.setTotalReservations(0);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            if (r.getId() == null) r.setId("new-id");
            return r;
        });
        when(reservationRepository.findByRestaurantIdAndStatusInAndReservationDateTimeBetween(
            any(), anyCollection(), any(), any())).thenAnswer(inv -> stored);
        givenAvailable(true);
    }
    
    private void givenAvailable(boolean available) {
        AvailabilityResult result = new AvailabilityResult(available, "r1", TOMORROW_20H, 2, available ? 8 : 0,
            available ? Reason.AVAILABLE : Reason.NOT_ENOUGH_SEATS,
            available ? List.of() : List.of(TOMORROW_20H.plusHours(1)));
        when(availabilityService.checkAvailability(any(), any(), anyInt(), any())).thenReturn(result);
    }
    
    private Reservation request(LocalDateTime at, Integer party) {
        Reservation r = new Reservation();
        r.setRestaurantId("r1");
        r.setReservationDateTime(at);
        r.setPartySize(party);
        return r;
    }
    
    private Reservation storedReservation(String id, String owner, ReservationStatus status) {
        Reservation r = request(TOMORROW_20H, 2);
        r.setId(id);
        r.setUserId(owner);
        r.setStatus(status);
        when(reservationRepository.findById(id)).thenReturn(Optional.of(r));
        return r;
    }
    
    // --- creation ---
    
    @Test
    void createsValidReservationAsPending() {
        Reservation saved = service.createReservation("u1", request(TOMORROW_20H, 2));
        
        assertThat(saved.getId()).isEqualTo("new-id");
        assertThat(saved.getUserId()).isEqualTo("u1");
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(user.getTotalReservations()).isEqualTo(1);
        verify(availabilityService).checkAvailability(eq("r1"), eq(TOMORROW_20H), eq(2), isNull());
    }
    
    @Test
    void rejectsPastDate() {
        assertThatThrownBy(() -> service.createReservation("u1", request(LocalDateTime.of(2026, 10, 15, 11, 0), 2)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("futur");
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void rejectsMissingDate() {
        assertThatThrownBy(() -> service.createReservation("u1", request(null, 2)))
            .isInstanceOf(BadRequestException.class);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 21, 100})
    void rejectsInvalidPartySize(int party) {
        assertThatThrownBy(() -> service.createReservation("u1", request(TOMORROW_20H, party)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("nombre de personnes");
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void rejectsUnavailableSlotWithVerifiedAlternatives() {
        givenAvailable(false);
        assertThatThrownBy(() -> service.createReservation("u1", request(TOMORROW_20H, 2)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("plus assez de places")
            .hasMessageContaining("16/10/2026 21:00");
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void rejectsDuplicateReservationOfSameUser() {
        Reservation mine = request(TOMORROW_20H.plusMinutes(30), 2);
        mine.setId("existing");
        mine.setUserId("u1");
        mine.setStatus(ReservationStatus.PENDING);
        stored.add(mine);
        
        assertThatThrownBy(() -> service.createReservation("u1", request(TOMORROW_20H, 2)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("déjà une réservation");
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void otherUsersReservationIsNotADuplicate() {
        Reservation other = request(TOMORROW_20H, 2);
        other.setId("x");
        other.setUserId("u2");
        stored.add(other);
        assertThat(service.createReservation("u1", request(TOMORROW_20H, 2)).getStatus())
            .isEqualTo(ReservationStatus.PENDING);
    }
    
    @Test
    void unknownUserThrowsNotFound() {
        assertThatThrownBy(() -> service.createReservation("ghost", request(TOMORROW_20H, 2)))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    // --- modification ---
    
    @Test
    void ownerCanModifyReservation() {
        Reservation r = storedReservation("res1", "u1", ReservationStatus.CONFIRMED);
        stored.add(r); // its own slot must not count as a duplicate
        
        Reservation updated = service.updateReservation("res1", "u1", "r1", TOMORROW_20H.plusHours(1), 4, "fenêtre");
        
        assertThat(updated.getPartySize()).isEqualTo(4);
        assertThat(updated.getReservationDateTime()).isEqualTo(TOMORROW_20H.plusHours(1));
        assertThat(updated.getSpecialRequests()).isEqualTo("fenêtre");
        verify(availabilityService).checkAvailability("r1", TOMORROW_20H.plusHours(1), 4, "res1");
    }
    
    @Test
    void otherUserCannotModifyReservation() {
        storedReservation("res1", "u1", ReservationStatus.PENDING);
        assertThatThrownBy(() -> service.updateReservation("res1", "intruder", "r1", TOMORROW_20H, 2, null))
            .isInstanceOf(ForbiddenException.class);
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void cannotModifyToUnavailableSlot() {
        storedReservation("res1", "u1", ReservationStatus.PENDING);
        givenAvailable(false);
        assertThatThrownBy(() -> service.updateReservation("res1", "u1", "r1", TOMORROW_20H, 2, null))
            .isInstanceOf(ConflictException.class);
    }
    
    @Test
    void cannotModifyCancelledReservation() {
        storedReservation("res1", "u1", ReservationStatus.CANCELLED);
        assertThatThrownBy(() -> service.updateReservation("res1", "u1", "r1", TOMORROW_20H, 2, null))
            .isInstanceOf(BadRequestException.class);
    }
    
    // --- cancellation ---
    
    @Test
    void ownerCanCancelReservation() {
        storedReservation("res1", "u1", ReservationStatus.CONFIRMED);
        assertThat(service.cancelReservation("res1", "u1").getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
    
    @Test
    void otherUserCannotCancelReservation() {
        storedReservation("res1", "u1", ReservationStatus.CONFIRMED);
        assertThatThrownBy(() -> service.cancelReservation("res1", "intruder"))
            .isInstanceOf(ForbiddenException.class);
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    void cannotCancelTwice() {
        storedReservation("res1", "u1", ReservationStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancelReservation("res1", "u1"))
            .isInstanceOf(BadRequestException.class);
    }
    
    @Test
    void cancellingUnknownReservationThrowsNotFound() {
        when(reservationRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelReservation("nope", "u1"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
