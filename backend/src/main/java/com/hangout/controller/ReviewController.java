package com.hangout.controller;

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
        // Implementation needed
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewResponse>> getRestaurantReviews(@PathVariable String restaurantId) {
        return ResponseEntity.ok(reviewService.getRestaurantReviews(restaurantId));
    }
    
    @PostMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String restaurantId,
            @RequestParam String userId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(restaurantId, userId, request));
    }
    
    @PutMapping("/{reviewId}/helpful")
    public ResponseEntity<Void> markHelpful(
            @PathVariable String reviewId,
            @RequestParam String userId) {
        reviewService.markHelpful(reviewId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String reviewId,
            @RequestParam String userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok().build();
    }
}
