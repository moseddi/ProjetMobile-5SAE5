package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.LoyaltyOverview;
import com.hangout.dto.VoucherResponse;
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
        return ResponseEntity.ok(Map.of("points", loyaltyService.getBalance(SecurityUtils.getCurrentUserId())));
    }

    /** Total points, daily games status, and per-restaurant balances with what each offer needs. */
    @GetMapping("/overview")
    public ResponseEntity<LoyaltyOverview> overview() {
        return ResponseEntity.ok(loyaltyService.getOverview(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PointTransaction>> getHistory() {
        return ResponseEntity.ok(loyaltyService.getHistory(SecurityUtils.getCurrentUserId()));
    }

    /** Exchange a restaurant's points for a reward voucher. */
    @PostMapping("/redeem")
    public ResponseEntity<VoucherResponse> redeem(@RequestBody Map<String, String> body) {
        String restaurantId = body.get("restaurantId");
        String reward = body.get("rewardType");
        if (restaurantId == null || restaurantId.isBlank() || reward == null || reward.isBlank()) {
            throw new BadRequestException("restaurantId and rewardType are required");
        }
        return ResponseEntity.ok(loyaltyService.redeem(SecurityUtils.getCurrentUserId(), restaurantId, reward));
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> vouchers() {
        return ResponseEntity.ok(loyaltyService.getVouchers(SecurityUtils.getCurrentUserId()));
    }
}
