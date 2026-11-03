package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.EventRegistrationRequest;
import com.hangout.dto.EventResponse;
import com.hangout.dto.ReservationResponse;
import com.hangout.model.Event;
import com.hangout.service.EventService;
import com.hangout.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAll() {
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<EventResponse>> getByRestaurant(@PathVariable String restaurantId) {
        return ResponseEntity.ok(eventService.getByRestaurant(restaurantId));
    }

    /**
     * Registering for an event creates a reservation awaiting the restaurant
     * owner's confirmation, just like a table booking - it does not instantly
     * confirm attendance. The event's spot is held immediately so the last
     * spot can't be double-booked while the owner decides.
     */
    @PostMapping("/{id}/register")
    public ResponseEntity<ReservationResponse> register(
            @PathVariable String id,
            @Valid @RequestBody EventRegistrationRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        reservationService.assertNotAlreadyRegisteredForEvent(userId, id);
        Event event = eventService.reserveSpots(id, request.getPartySize());
        return ResponseEntity.ok(reservationService.createForEvent(userId, event, request));
    }
}
