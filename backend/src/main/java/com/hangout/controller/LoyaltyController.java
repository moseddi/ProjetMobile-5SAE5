package com.hangout.controller;

import com.hangout.dto.loyalty.*;
import com.hangout.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/dashboard")
    public ResponseEntity<LoyaltyDashboardResponse> getDashboard(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getDashboard(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionDto>> getTransactions(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getTransactions(userId));
    }

    @GetMapping("/rewards")
    public ResponseEntity<List<RewardDto>> getRewards(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getRewards(userId));
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    public ResponseEntity<RedemptionDto> redeemReward(@PathVariable String rewardId, @RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.redeemReward(userId, rewardId));
    }

    @GetMapping("/redemptions")
    public ResponseEntity<List<RedemptionDto>> getRedemptions(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getRedemptions(userId));
    }

    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeDto>> getTodaysChallenges(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getTodaysChallenges(userId));
    }

    @GetMapping("/games")
    public ResponseEntity<List<GameCatalogEntry>> getGames(@RequestParam String userId) {
        return ResponseEntity.ok(loyaltyService.getGameCatalog(userId));
    }
}
