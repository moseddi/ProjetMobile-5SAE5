package com.hangout.service;

import com.hangout.config.ReservationProperties;
import com.hangout.dto.AvailabilityResult;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ConflictException;
import com.hangout.exception.ForbiddenException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.User;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Single entry point for every reservation write. The REST API and the chatbot both go through it,
 * so the same validation, availability and ownership rules always apply.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final ReservationProperties properties;
    private final Clock clock;
    
    public List<Reservation> getUserReservations(String userId) {
        return reservationRepository.findByUserId(userId);
    }
    
    public Reservation getReservationById(String id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
    
    public Reservation getReservationForUser(String id, String requesterUserId) {
        Reservation reservation = getReservationById(id);
        checkOwner(reservation, requesterUserId);
        return reservation;
    }
    
    @Transactional
    public Reservation createReservation(String userId, Reservation reservation) {
        validateRequest(reservation.getReservationDateTime(), reservation.getPartySize());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        checkNoDuplicate(userId, reservation.getRestaurantId(), reservation.getReservationDateTime(), null);
        checkAvailable(reservation.getRestaurantId(), reservation.getReservationDateTime(),
            reservation.getPartySize(), null);
        
        reservation.setId(null);
        reservation.setUserId(userId);
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        Reservation saved = reservationRepository.save(reservation);
        
        user.setTotalReservations(user.getTotalReservations() + 1);
        userRepository.save(user);
        
        return saved;
    }
    
    @Transactional
    public Reservation updateReservation(String id, String requesterUserId, String restaurantId,
                                         LocalDateTime dateTime, Integer partySize, String specialRequests) {
        Reservation reservation = getReservationForUser(id, requesterUserId);
        checkModifiable(reservation);
        validateRequest(dateTime, partySize);
        checkNoDuplicate(requesterUserId, restaurantId, dateTime, id);
        checkAvailable(restaurantId, dateTime, partySize, id);
        
        reservation.setRestaurantId(restaurantId);
        reservation.setReservationDateTime(dateTime);
        reservation.setPartySize(partySize);
        reservation.setSpecialRequests(specialRequests);
        return reservationRepository.save(reservation);
    }
    
    @Transactional
    public Reservation cancelReservation(String id, String requesterUserId) {
        Reservation reservation = getReservationForUser(id, requesterUserId);
        checkModifiable(reservation);
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }
    
    void validateRequest(LocalDateTime dateTime, Integer partySize) {
        if (dateTime == null) {
            throw new BadRequestException("La date et l'heure de la réservation sont obligatoires");
        }
        if (!dateTime.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("La réservation doit être dans le futur");
        }
        if (partySize == null || partySize < properties.getMinPartySize() || partySize > properties.getMaxPartySize()) {
            throw new BadRequestException("Le nombre de personnes doit être compris entre "
                + properties.getMinPartySize() + " et " + properties.getMaxPartySize());
        }
    }
    
    private void checkOwner(Reservation reservation, String requesterUserId) {
        if (requesterUserId == null || !requesterUserId.equals(reservation.getUserId())) {
            throw new ForbiddenException("Cette réservation ne vous appartient pas");
        }
    }
    
    private void checkModifiable(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED
                || reservation.getStatus() == Reservation.ReservationStatus.COMPLETED) {
            throw new BadRequestException("Une réservation " + reservation.getStatus() + " ne peut plus être modifiée");
        }
    }
    
    private void checkNoDuplicate(String userId, String restaurantId, LocalDateTime dateTime, String excludeId) {
        Duration slot = Duration.ofMinutes(properties.getSlotDurationMinutes());
        boolean duplicate = reservationRepository
            .findByRestaurantIdAndStatusInAndReservationDateTimeBetween(
                restaurantId, AvailabilityService.ACTIVE_STATUSES, dateTime.minus(slot), dateTime.plus(slot))
            .stream()
            .anyMatch(r -> userId.equals(r.getUserId()) && !Objects.equals(r.getId(), excludeId));
        if (duplicate) {
            throw new ConflictException("Vous avez déjà une réservation dans ce restaurant sur ce créneau");
        }
    }
    
    private void checkAvailable(String restaurantId, LocalDateTime dateTime, int partySize, String excludeId) {
        AvailabilityResult result = availabilityService.checkAvailability(restaurantId, dateTime, partySize, excludeId);
        if (result.available()) {
            return;
        }
        String message = switch (result.reason()) {
            case RESTAURANT_CLOSED -> "Ce restaurant n'accepte pas de réservation actuellement";
            case PARTY_TOO_LARGE_FOR_RESTAURANT -> "Le restaurant ne peut pas accueillir " + partySize + " personnes";
            case OUTSIDE_OPENING_HOURS -> "Ce créneau est en dehors des horaires de réservation";
            default -> "Ce créneau n'a plus assez de places disponibles";
        };
        if (!result.alternatives().isEmpty()) {
            message += ". Créneaux disponibles : " + result.alternatives().stream()
                .map(SLOT_FORMAT::format).collect(Collectors.joining(", "));
        }
        throw new ConflictException(message);
    }
}
