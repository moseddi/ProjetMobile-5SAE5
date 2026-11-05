package com.hangout.controller;

import com.hangout.util.Newest;
import com.hangout.config.SecurityUtils;
import com.hangout.dto.AdminStats;
import com.hangout.dto.ReviewResponse;
import com.hangout.dto.UserDto;
import com.hangout.dto.UserMapper;
import com.hangout.exception.BadRequestException;
import com.hangout.model.Reservation;
import com.hangout.model.User;
import com.hangout.repository.ReservationRepository;
import com.hangout.service.AdminService;
import com.hangout.service.EventService;
import com.hangout.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReviewService reviewService;
    private final EventService eventService;
    private final ReservationRepository reservationRepository;
    private final com.hangout.service.OwnerApplicationService ownerApplicationService;

    // ---- Users ----

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers(@RequestParam(required = false) String role) {
        User.Role roleFilter = null;
        if (role != null && !role.isBlank()) {
            try {
                roleFilter = User.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role: " + role);
            }
        }
        List<UserDto> users = adminService.getUsers(roleFilter).stream()
            .map(UserMapper::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(UserMapper.toDto(adminService.getUser(id)));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserDto> setStatus(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            throw new BadRequestException("active is required");
        }
        if (id.equals(SecurityUtils.getCurrentUserId()) && !active) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        return ResponseEntity.ok(UserMapper.toDto(adminService.setActive(id, active)));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDto> setRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        String roleValue = body.get("role");
        if (roleValue == null) {
            throw new BadRequestException("role is required");
        }
        User.Role role;
        try {
            role = User.Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleValue);
        }
        return ResponseEntity.ok(UserMapper.toDto(adminService.setRole(id, role)));
    }

    @PostMapping("/users/{id}/assign-owner")
    public ResponseEntity<UserDto> assignOwner(@PathVariable String id, @RequestBody Map<String, String> body) {
        String restaurantId = body.get("restaurantId");
        if (restaurantId == null) {
            throw new BadRequestException("restaurantId is required");
        }
        return ResponseEntity.ok(UserMapper.toDto(adminService.assignOwner(id, restaurantId)));
    }

    @PostMapping("/users/{id}/unassign-owner")
    public ResponseEntity<UserDto> unassignOwner(@PathVariable String id) {
        return ResponseEntity.ok(UserMapper.toDto(adminService.unassignOwner(id)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (id.equals(SecurityUtils.getCurrentUserId())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // ---- Restaurants (with owner) ----

    @PostMapping("/restaurants")
    public ResponseEntity<com.hangout.model.Restaurant> createRestaurantWithOwner(
            @RequestBody com.hangout.dto.RestaurantWithOwnerRequest request) {
        return ResponseEntity.ok(adminService.createRestaurantWithOwner(request));
    }

    // ---- Restaurant owner requests ----

    @GetMapping("/owner-applications")
    public ResponseEntity<List<com.hangout.dto.OwnerApplicationResponse>> getOwnerApplications(
            @RequestParam(required = false) String status) {
        com.hangout.model.OwnerApplication.Status filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = com.hangout.model.OwnerApplication.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }
        return ResponseEntity.ok(ownerApplicationService.list(filter));
    }

    @PostMapping("/owner-applications/{id}/accept")
    public ResponseEntity<com.hangout.dto.OwnerApplicationResponse> acceptOwnerApplication(@PathVariable String id) {
        return ResponseEntity.ok(ownerApplicationService.accept(id));
    }

    @PostMapping("/owner-applications/{id}/reject")
    public ResponseEntity<com.hangout.dto.OwnerApplicationResponse> rejectOwnerApplication(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(ownerApplicationService.reject(id, body != null ? body.get("reason") : null));
    }

    // ---- Moderation / oversight ----

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(reservationRepository.findAll().stream()
            .sorted(com.hangout.util.Newest.first(Reservation::getId))
            .collect(Collectors.toList()));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id, SecurityUtils.getCurrentUserId(), true);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        eventService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ---- Stats ----

    @GetMapping("/stats")
    public ResponseEntity<AdminStats> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
