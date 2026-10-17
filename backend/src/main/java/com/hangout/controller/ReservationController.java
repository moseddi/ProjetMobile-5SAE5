package com.hangout.controller;

import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.ReservationRequest;
import com.hangout.model.Reservation;
import com.hangout.service.AvailabilityService;
import com.hangout.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

// The user always comes from the verified JWT (see JwtAuthFilter), never from a request parameter
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    
    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;
    
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResult> checkAvailability(
            @RequestParam String restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam int partySize) {
        return ResponseEntity.ok(availabilityService.checkAvailability(restaurantId, dateTime, partySize));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable String id,
                                                          @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(reservationService.getReservationForUser(id, userId));
    }
    
    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setRestaurantId(request.getRestaurantId());
        reservation.setReservationDateTime(request.getReservationDateTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setSpecialRequests(request.getSpecialRequests());
        
        return ResponseEntity.ok(reservationService.createReservation(userId, reservation));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.updateReservation(id, userId, request.getRestaurantId(),
            request.getReservationDateTime(), request.getPartySize(), request.getSpecialRequests()));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String id, @AuthenticationPrincipal String userId) {
        reservationService.cancelReservation(id, userId);
        return ResponseEntity.ok().build();
    }
}
