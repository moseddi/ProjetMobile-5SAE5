package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.ReservationRequest;
import com.hangout.dto.ReservationResponse;
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

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reservationService.getUserReservations(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable String id) {
        var reservation = reservationService.getReservationById(id);
        reservationService.assertCanView(reservation);
        return ResponseEntity.ok(reservationService.getResponseById(id));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reservationService.createReservation(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        reservationService.cancelOwnReservation(id, userId);
        return ResponseEntity.ok().build();
    }
}
