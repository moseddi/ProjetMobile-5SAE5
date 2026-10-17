package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

@Data
public class LoyaltyDashboardResponse {
    private Integer points;
    private String tier;
    private String nextTier;
    private Integer pointsToNextTier;
    private Double tierProgressPercent;
    private List<PointTransactionDto> recentTransactions;
    private Integer affordableRewardsCount;
    private List<ChallengeDto> todaysChallenges;
    private List<GameCatalogEntry> games;
}
