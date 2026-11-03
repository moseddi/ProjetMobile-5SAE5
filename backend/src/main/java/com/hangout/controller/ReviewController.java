package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewResponse>> getRestaurantReviews(@PathVariable String restaurantId) {
        return ResponseEntity.ok(reviewService.getRestaurantReviews(restaurantId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable String userId) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId));
    }

    @PostMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String restaurantId,
            @Valid @RequestBody ReviewRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.createReview(restaurantId, userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.updateOwnReview(id, userId, request));
    }

    @PutMapping("/{reviewId}/helpful")
    public ResponseEntity<Void> markHelpful(@PathVariable String reviewId) {
        reviewService.markHelpful(reviewId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable String reviewId) {
        String userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        reviewService.deleteReview(reviewId, userId, isAdmin);
        return ResponseEntity.ok().build();
    }
}
