package com.hangout.service;

import com.hangout.dto.loyalty.*;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.*;
import com.hangout.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative points ledger: every balance mutation goes through
 * {@link #creditPoints} or {@link #debitPoints}, which atomically update the
 * user's cached balance (via a Mongo findAndModify, to avoid lost updates
 * under concurrent requests) and append an immutable {@link PointTransaction}.
 * Nothing else is allowed to write {@code User.loyaltyPoints} directly.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final RewardRepository rewardRepository;
    private final RedemptionRepository redemptionRepository;
    private final GameResultRepository gameResultRepository;
    private final MongoTemplate mongoTemplate;

    private static final int[] TIER_THRESHOLDS = {0, 500, 1500, 3000};
    private static final String[] TIER_NAMES = {"Bronze", "Silver", "Gold", "Platinum"};

    // ---------------------------------------------------------------- ledger

    public int creditPoints(String userId, int amount, PointTransaction.TransactionType type, String description, String relatedId) {
        if (amount <= 0) {
            throw new BadRequestException("Credit amount must be positive");
        }
        User updated = mongoTemplate.findAndModify(
            Query.query(Criteria.where("_id").is(userId)),
            new Update().inc("loyaltyPoints", amount),
            FindAndModifyOptions.options().returnNew(true),
            User.class
        );
        if (updated == null) {
            throw new ResourceNotFoundException("User not found");
        }
        recordTransaction(userId, amount, type, description, relatedId);
        return updated.getLoyaltyPoints();
    }

    public int debitPoints(String userId, int amount, PointTransaction.TransactionType type, String description, String relatedId) {
        if (amount <= 0) {
            throw new BadRequestException("Debit amount must be positive");
        }
        // Guard the decrement in the query itself so concurrent requests can never
        // push the balance below zero.
        User updated = mongoTemplate.findAndModify(
            Query.query(Criteria.where("_id").is(userId).and("loyaltyPoints").gte(amount)),
            new Update().inc("loyaltyPoints", -amount),
            FindAndModifyOptions.options().returnNew(true),
            User.class
        );
        if (updated == null) {
            if (!userRepository.existsById(userId)) {
                throw new ResourceNotFoundException("User not found");
            }
            throw new BadRequestException("Insufficient points balance");
        }
        recordTransaction(userId, -amount, type, description, relatedId);
        return updated.getLoyaltyPoints();
    }

    /** Awards points for a relatedId exactly once (idempotency guard for daily challenges, etc). */
    public boolean creditOnce(String userId, int amount, PointTransaction.TransactionType type, String description, String relatedId) {
        if (pointTransactionRepository.findByUserIdAndTypeAndRelatedId(userId, type, relatedId).isPresent()) {
            return false;
        }
        creditPoints(userId, amount, type, description, relatedId);
        return true;
    }

    private void recordTransaction(String userId, int points, PointTransaction.TransactionType type, String description, String relatedId) {
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setPoints(points);
        tx.setType(type);
        tx.setDescription(description);
        tx.setRelatedId(relatedId);
        pointTransactionRepository.save(tx);
    }

    // ------------------------------------------------------------------ tier

    public String tierFor(int points) {
        String tier = TIER_NAMES[0];
        for (int i = 0; i < TIER_THRESHOLDS.length; i++) {
            if (points >= TIER_THRESHOLDS[i]) {
                tier = TIER_NAMES[i];
            }
        }
        return tier;
    }

    private int tierIndex(int points) {
        int idx = 0;
        for (int i = 0; i < TIER_THRESHOLDS.length; i++) {
            if (points >= TIER_THRESHOLDS[i]) idx = i;
        }
        return idx;
    }

    // ------------------------------------------------------------- dashboard

    public LoyaltyDashboardResponse getDashboard(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int points = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;

        LoyaltyDashboardResponse dto = new LoyaltyDashboardResponse();
        dto.setPoints(points);
        int idx = tierIndex(points);
        dto.setTier(TIER_NAMES[idx]);
        if (idx < TIER_NAMES.length - 1) {
            dto.setNextTier(TIER_NAMES[idx + 1]);
            int floor = TIER_THRESHOLDS[idx];
            int ceiling = TIER_THRESHOLDS[idx + 1];
            dto.setPointsToNextTier(ceiling - points);
            dto.setTierProgressPercent(Math.min(100.0, 100.0 * (points - floor) / (ceiling - floor)));
        } else {
            dto.setNextTier(null);
            dto.setPointsToNextTier(0);
            dto.setTierProgressPercent(100.0);
        }

        dto.setRecentTransactions(pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .limit(10)
            .map(this::toTransactionDto)
            .collect(Collectors.toList()));

        long affordable = rewardRepository.findByActiveTrue().stream()
            .filter(r -> r.getPointsCost() != null && r.getPointsCost() <= points)
            .count();
        dto.setAffordableRewardsCount((int) affordable);

        dto.setTodaysChallenges(getTodaysChallenges(userId));
        dto.setGames(getGameCatalog(userId));
        return dto;
    }

    public List<PointTransactionDto> getTransactions(String userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toTransactionDto)
            .collect(Collectors.toList());
    }

    private PointTransactionDto toTransactionDto(PointTransaction tx) {
        PointTransactionDto dto = new PointTransactionDto();
        dto.setId(tx.getId());
        dto.setPoints(tx.getPoints());
        dto.setType(tx.getType().name());
        dto.setDescription(tx.getDescription());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }

    // ------------------------------------------------------------- rewards

    public List<RewardDto> getRewards(String userId) {
        int points = currentBalance(userId);
        return rewardRepository.findByActiveTrueOrderByPointsCostAsc().stream()
            .map(r -> toRewardDto(r, points))
            .collect(Collectors.toList());
    }

    private RewardDto toRewardDto(Reward r, int currentPoints) {
        RewardDto dto = new RewardDto();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setDescription(r.getDescription());
        dto.setCategory(r.getCategory().name());
        dto.setPointsCost(r.getPointsCost());
        dto.setStock(r.getStock());
        dto.setImageUrl(r.getImageUrl());
        dto.setTermsAndConditions(r.getTermsAndConditions());
        dto.setRestaurantId(r.getRestaurantId());
        dto.setExpiryDate(r.getExpiryDate());
        boolean affordable = r.getPointsCost() != null && r.getPointsCost() <= currentPoints
            && (r.getStock() == null || r.getStock() > 0);
        dto.setAffordable(affordable);
        dto.setPointsMissing(Math.max(0, (r.getPointsCost() != null ? r.getPointsCost() : 0) - currentPoints));
        return dto;
    }

    @Transactional
    public RedemptionDto redeemReward(String userId, String rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
            .orElseThrow(() -> new ResourceNotFoundException("Reward not found"));
        if (!Boolean.TRUE.equals(reward.getActive())) {
            throw new BadRequestException("This reward is no longer available");
        }
        if (reward.getExpiryDate() != null && reward.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reward has expired");
        }
        if (reward.getStock() != null && reward.getStock() <= 0) {
            throw new BadRequestException("This reward is out of stock");
        }

        // Atomically decrement stock (when tracked) so two concurrent redemptions
        // cannot both succeed on the last unit.
        if (reward.getStock() != null) {
            Reward updatedStock = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(rewardId).and("stock").gt(0)),
                new Update().inc("stock", -1),
                FindAndModifyOptions.options().returnNew(true),
                Reward.class
            );
            if (updatedStock == null) {
                throw new BadRequestException("This reward is out of stock");
            }
        }

        debitPoints(userId, reward.getPointsCost(), PointTransaction.TransactionType.REDEEM,
            "Redeemed: " + reward.getName(), rewardId);

        Redemption redemption = new Redemption();
        redemption.setUserId(userId);
        redemption.setRewardId(reward.getId());
        redemption.setRewardName(reward.getName());
        redemption.setPointsCost(reward.getPointsCost());
        redemption.setStatus(Redemption.RedemptionStatus.COMPLETED);
        redemption = redemptionRepository.save(redemption);

        return toRedemptionDto(redemption);
    }

    public List<RedemptionDto> getRedemptions(String userId) {
        return redemptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toRedemptionDto)
            .collect(Collectors.toList());
    }

    private RedemptionDto toRedemptionDto(Redemption r) {
        RedemptionDto dto = new RedemptionDto();
        dto.setId(r.getId());
        dto.setRewardId(r.getRewardId());
        dto.setRewardName(r.getRewardName());
        dto.setPointsCost(r.getPointsCost());
        dto.setStatus(r.getStatus().name());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public int currentBalance(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
    }

    // ----------------------------------------------------------- game catalog

    private static final int DAILY_LIMIT_QUIZ = 1;
    private static final int DAILY_LIMIT_MEMORY = 3;
    private static final int DAILY_LIMIT_SCRATCH = 1;
    private static final int DAILY_LIMIT_WHEEL = 1;

    public List<GameCatalogEntry> getGameCatalog(String userId) {
        LocalDateTime startOfDay = startOfToday();
        List<GameCatalogEntry> games = new ArrayList<>();

        games.add(buildCatalogEntry(userId, GameResult.GameType.QUIZ, startOfDay,
            "quiz", "Quiz du jour", "Reponds a 5 questions et gagne des points selon ton score.",
            "Trivia", "EASY", 3, "quiz-icon", "5 questions, 1 tentative par jour. 10 points par bonne reponse.",
            "10-50", DAILY_LIMIT_QUIZ));

        games.add(buildCatalogEntry(userId, GameResult.GameType.CARD_MEMORY, startOfDay,
            "memory", "Memory Match", "Retrouve les paires de cartes le plus vite possible.",
            "Memory", "MEDIUM", 2, "memory-icon", "8 paires. Moins tu fais de coups, plus tu gagnes de points.",
            "10-40", DAILY_LIMIT_MEMORY));

        games.add(buildCatalogEntry(userId, GameResult.GameType.SCRATCH_CARD, startOfDay,
            "scratch", "Carte a gratter", "Gratte la carte pour reveler ta recompense.",
            "Chance", "EASY", 1, "scratch-icon", "1 carte gratuite par jour. Recompenses de 0 a 100 points.",
            "0-100", DAILY_LIMIT_SCRATCH));

        games.add(buildCatalogEntry(userId, GameResult.GameType.DAILY_WHEEL, startOfDay,
            "wheel", "Roue quotidienne", "Fais tourner la roue pour tenter de gagner des points.",
            "Chance", "EASY", 1, "wheel-icon", "1 tour gratuit par jour. Segments de 0 a 150 points.",
            "0-150", DAILY_LIMIT_WHEEL));

        return games;
    }

    private GameCatalogEntry buildCatalogEntry(String userId, GameResult.GameType type, LocalDateTime startOfDay,
                                                String id, String name, String description, String category,
                                                String difficulty, int durationMin, String icon, String rules,
                                                String pointsRange, int dailyLimit) {
        int usedToday = gameResultRepository.findByUserIdAndGameTypeAndPlayedAtAfter(userId, type, startOfDay).size();
        return new GameCatalogEntry(id, name, description, category, difficulty, durationMin, icon, rules,
            pointsRange, dailyLimit, usedToday, usedToday < dailyLimit);
    }

    public void assertDailyLimitNotExceeded(String userId, GameResult.GameType type, int dailyLimit) {
        int usedToday = gameResultRepository.findByUserIdAndGameTypeAndPlayedAtAfter(userId, type, startOfToday()).size();
        if (usedToday >= dailyLimit) {
            throw new BadRequestException("Daily limit reached for this game. Come back tomorrow!");
        }
    }

    public int attemptsToday(String userId, GameResult.GameType type) {
        return gameResultRepository.findByUserIdAndGameTypeAndPlayedAtAfter(userId, type, startOfToday()).size();
    }

    // --------------------------------------------------------------- challenges

    public List<ChallengeDto> getTodaysChallenges(String userId) {
        LocalDateTime startOfDay = startOfToday();
        List<GameResult> playedToday = gameResultRepository.findByUserIdAndPlayedAtAfter(userId, startOfDay);
        long distinctGamesPlayed = playedToday.stream().map(GameResult::getGameType).distinct().count();
        String todayKey = LocalDate.now(ZoneId.systemDefault()).toString();

        ChallengeDto playOne = new ChallengeDto(
            "play_one:" + todayKey, "Joueur du jour", "Joue a un jeu aujourd'hui.",
            20, Math.min(1, playedToday.size()), 1, !playedToday.isEmpty());

        ChallengeDto playTwoDifferent = new ChallengeDto(
            "play_two_different:" + todayKey, "Explorateur", "Joue a 2 jeux differents aujourd'hui.",
            40, (int) Math.min(2, distinctGamesPlayed), 2, distinctGamesPlayed >= 2);

        return List.of(playOne, playTwoDifferent);
    }

    /** Awards the challenge bonus if just completed and not already granted today. Returns true if newly awarded. */
    public boolean claimChallengeIfCompleted(String userId, ChallengeDto challenge) {
        if (!challenge.isCompleted()) {
            return false;
        }
        return creditOnce(userId, challenge.getPointsReward(), PointTransaction.TransactionType.EARN_CHALLENGE,
            challenge.getTitle(), challenge.getId());
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
    }

    /** Called right after a game result is recorded, so challenge bonuses are only
     *  ever granted as a consequence of a real state change — never from a GET. */
    public void checkAndClaimChallenges(String userId) {
        for (ChallengeDto challenge : getTodaysChallenges(userId)) {
            claimChallengeIfCompleted(userId, challenge);
        }
    }
}
