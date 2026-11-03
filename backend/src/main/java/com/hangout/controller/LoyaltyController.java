package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.exception.BadRequestException;
import com.hangout.model.PointTransaction;
import com.hangout.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/points")
    public ResponseEntity<Map<String, Integer>> getPoints() {
        int balance = loyaltyService.getBalance(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("points", balance));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PointTransaction>> getHistory() {
        return ResponseEntity.ok(loyaltyService.getHistory(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<Void> redeem(@RequestBody Map<String, Object> body) {
        Object pointsValue = body.get("points");
        if (!(pointsValue instanceof Number)) {
            throw new BadRequestException("points is required");
        }
        int points = ((Number) pointsValue).intValue();
        String description = body.getOrDefault("description", "Points redeemed").toString();
        loyaltyService.redeemPoints(SecurityUtils.getCurrentUserId(), points, description);
        return ResponseEntity.ok().build();
    }
}
