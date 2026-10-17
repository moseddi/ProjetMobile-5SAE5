package com.hangout.controller;

import com.hangout.dto.loyalty.RestaurantRecommendationResponse;
import com.hangout.service.RestaurantRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only: finding recommendations never changes a balance, a reward, or any other state.
 * The user always comes from the verified JWT (see JwtAuthFilter), never from a request parameter.
 */
@RestController
@RequestMapping("/api/loyalty/recommendations")
@RequiredArgsConstructor
public class LoyaltyRecommendationController {

    private final RestaurantRecommendationService recommendationService;

    @GetMapping("/restaurants")
    public ResponseEntity<RestaurantRecommendationResponse> recommendRestaurants(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String goal,
            @RequestParam(required = false) String message) {
        return ResponseEntity.ok(recommendationService.recommend(userId, goal, message));
    }
}
