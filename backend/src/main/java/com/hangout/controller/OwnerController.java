package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.EventRequest;
import com.hangout.dto.EventResponse;
import com.hangout.dto.OwnerStats;
import com.hangout.dto.ReservationResponse;
import com.hangout.dto.ReviewResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.model.MenuItem;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.repository.EventRepository;
import com.hangout.repository.MenuItemRepository;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.model.Review;
import com.hangout.dto.MemberLookupResponse;
import com.hangout.dto.VoucherResponse;
import com.hangout.service.EventService;
import com.hangout.service.LoyaltyService;
import com.hangout.service.MenuItemService;
import com.hangout.service.ReservationService;
import com.hangout.service.RestaurantService;
import com.hangout.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;
    private final EventService eventService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final LoyaltyService loyaltyService;

    private final ReservationRepository reservationRepository;
    private final MenuItemRepository menuItemRepository;
    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;

    private String myId() {
        return SecurityUtils.getCurrentUserId();
    }

    private Restaurant myRestaurant() {
        return restaurantService.getOwnRestaurant(myId());
    }

    // ---- Cashier: scan a client's QR, validate rewards ----

    @GetMapping("/members/{code}")
    public ResponseEntity<MemberLookupResponse> lookupMember(@PathVariable String code) {
        return ResponseEntity.ok(loyaltyService.lookupMember(myRestaurant().getId(), code));
    }

    @PostMapping("/vouchers/{id}/use")
    public ResponseEntity<VoucherResponse> useVoucher(@PathVariable String id) {
        return ResponseEntity.ok(loyaltyService.useVoucher(myRestaurant().getId(), id));
    }

    // ---- Loyalty programme on/off ----

    @PutMapping("/loyalty")
    public ResponseEntity<Restaurant> setLoyalty(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new BadRequestException("enabled is required");
        }
        return ResponseEntity.ok(restaurantService.setOwnLoyaltyEnabled(myId(), enabled));
    }

    // ---- Restaurant profile ----

    @GetMapping("/restaurant")
    public ResponseEntity<Restaurant> getMyRestaurant() {
        return ResponseEntity.ok(myRestaurant());
    }

    @PutMapping("/restaurant")
    public ResponseEntity<Restaurant> updateMyRestaurant(@RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.updateOwnRestaurant(myId(), restaurant));
    }

    // ---- Menu management ----

    @GetMapping("/menu-items")
    public ResponseEntity<List<MenuItem>> getMyMenuItems() {
        return ResponseEntity.ok(menuItemService.getByRestaurant(myRestaurant().getId()));
    }

    @PostMapping("/menu-items")
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItem item) {
        return ResponseEntity.ok(menuItemService.create(myRestaurant().getId(), item));
    }

    @PutMapping("/menu-items/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable String id, @RequestBody MenuItem item) {
        menuItemService.assertOwns(id, myId());
        return ResponseEntity.ok(menuItemService.update(id, item));
    }

    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable String id) {
        menuItemService.assertOwns(id, myId());
        menuItemService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ---- Reservations ----

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        return ResponseEntity.ok(reservationService.getRestaurantReservations(myRestaurant().getId()));
    }

    @PutMapping("/reservations/{id}/status")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String statusValue = body.get("status");
        if (statusValue == null) {
            throw new BadRequestException("status is required");
        }
        Reservation.ReservationStatus status;
        try {
            status = Reservation.ReservationStatus.valueOf(statusValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + statusValue);
        }
        return ResponseEntity.ok(reservationService.updateStatus(id, myId(), status));
    }

    // ---- Events ----

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getMyEvents() {
        return ResponseEntity.ok(eventService.getByRestaurant(myRestaurant().getId()));
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.create(myRestaurant().getId(), request));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable String id, @Valid @RequestBody EventRequest request) {
        eventService.assertOwnsEvent(id, myId());
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        eventService.assertOwnsEvent(id, myId());
        eventService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ---- Reviews (read-only) ----

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getRestaurantReviews(myRestaurant().getId()));
    }

    // ---- Dashboard stats ----

    @GetMapping("/stats")
    public ResponseEntity<OwnerStats> getStats() {
        Restaurant restaurant = myRestaurant();
        List<Reservation> reservations = reservationRepository.findByRestaurantId(restaurant.getId());

        OwnerStats stats = new OwnerStats();
        stats.setTotalReservations(reservations.size());
        stats.setPendingReservations(reservations.stream()
            .filter(r -> r.getStatus() == Reservation.ReservationStatus.PENDING).count());
        stats.setConfirmedReservations(reservations.stream()
            .filter(r -> r.getStatus() == Reservation.ReservationStatus.CONFIRMED).count());
        stats.setCompletedReservations(reservations.stream()
            .filter(r -> r.getStatus() == Reservation.ReservationStatus.COMPLETED
                || r.getStatus() == Reservation.ReservationStatus.ARRIVED).count());
        stats.setTotalMenuItems(menuItemRepository.findByRestaurantId(restaurant.getId()).size());
        stats.setTotalEvents(eventRepository.findByRestaurantId(restaurant.getId()).size());
        stats.setTotalReviews(reviewRepository.countByRestaurantIdAndStatus(restaurant.getId(), Review.ReviewStatus.PUBLISHED));
        stats.setAverageRating(restaurant.getRating());

        return ResponseEntity.ok(stats);
    }
}
