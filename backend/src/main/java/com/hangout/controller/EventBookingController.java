package com.hangout.controller;

import com.hangout.dto.EventBookingRequest;
import com.hangout.model.EventBooking;
import com.hangout.service.EventBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// The user always comes from the verified JWT (see JwtAuthFilter), never from a request parameter
@RestController
@RequestMapping("/api/event-bookings")
@RequiredArgsConstructor
public class EventBookingController {

    private final EventBookingService eventBookingService;

    @PostMapping
    public ResponseEntity<EventBooking> book(@AuthenticationPrincipal String userId,
                                             @Valid @RequestBody EventBookingRequest request) {
        return ResponseEntity.ok(eventBookingService.bookEvent(userId, request.getEventId(), request.getPartySize()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id, @AuthenticationPrincipal String userId) {
        eventBookingService.cancelBooking(id, userId);
        return ResponseEntity.ok().build();
    }
}
