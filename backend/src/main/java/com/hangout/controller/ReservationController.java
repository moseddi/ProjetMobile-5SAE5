package com.hangout.controller;

import com.hangout.dto.ReservationRequest;
import com.hangout.model.Reservation;
import com.hangout.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    
    private final ReservationService reservationService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }
    
    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @RequestParam String userId,
            @Valid @RequestBody ReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setRestaurantId(request.getRestaurantId());
        reservation.setReservationDateTime(request.getReservationDateTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setSpecialRequests(request.getSpecialRequests());
        
        return ResponseEntity.ok(reservationService.createReservation(userId, reservation));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.ok().build();
    }
}
