package com.hangout.controller;

import com.hangout.exception.ForbiddenException;
import com.hangout.model.EventBooking;
import com.hangout.model.Reservation;
import com.hangout.model.User;
import com.hangout.repository.EventBookingRepository;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final EventBookingRepository eventBookingRepository;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{userId}/reservations")
    public ResponseEntity<List<Reservation>> getUserReservations(@PathVariable String userId,
                                                                 @AuthenticationPrincipal String currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("Vous ne pouvez consulter que vos propres réservations");
        }
        return ResponseEntity.ok(reservationRepository.findByUserId(userId));
    }

    @GetMapping("/{userId}/event-bookings")
    public ResponseEntity<List<EventBooking>> getUserEventBookings(@PathVariable String userId,
                                                                    @AuthenticationPrincipal String currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("Vous ne pouvez consulter que vos propres réservations");
        }
        return ResponseEntity.ok(eventBookingRepository.findByUserId(userId));
    }
}
