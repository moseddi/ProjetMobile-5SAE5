package com.hangout.service;

import com.hangout.dto.loyalty.RecommendationCandidate;
import com.hangout.dto.loyalty.RecommendationExplanations;
import com.hangout.dto.loyalty.RestaurantRecommendationCard;
import com.hangout.dto.loyalty.RestaurantRecommendationResponse;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.Reward;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks, deterministically, which restaurant + reward pairs are worth showing a customer
 * for spending their loyalty points, then hands that already-verified shortlist to
 * {@link LoyaltyAssistantService} only to be explained in natural language.
 *
 * <p>Every number in the response (balance, points cost, points missing, points remaining)
 * comes straight from {@link LoyaltyService}'s authoritative ledger and the real reward
 * catalog — the LLM never computes or alters any of it. This service never writes
 * anything: it is read-only by construction (no call to creditPoints/debitPoints/redeemReward).
 */
@Service
@RequiredArgsConstructor
public class RestaurantRecommendationService {

    public static final String GOAL_SPEND_NOW = "SPEND_NOW";
    public static final String GOAL_SAVE_FOR_PREMIUM = "SAVE_FOR_PREMIUM";

    private static final int MAX_RECOMMENDATIONS = 5;
    /** A reward is "within reach" for saving-for-premium purposes up to this fraction of the current balance. */
    private static final double SAVE_FOR_PREMIUM_REACH_FACTOR = 1.5;

    private final LoyaltyService loyaltyService;
    private final LoyaltyAssistantService assistantService;
    private final RewardRepository rewardRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    public RestaurantRecommendationResponse recommend(String userId, String goal, String message) {
        int balance = loyaltyService.currentBalance(userId);
        Map<String, Restaurant> restaurants = restaurantsById();
        String preferredCuisine = inferPreferredCuisine(userId, restaurants);

        List<RecommendationCandidate> eligible = buildEligibleCandidates(balance, restaurants, preferredCuisine);
        List<RecommendationCandidate> bestPerRestaurant = keepBestRewardPerRestaurant(eligible, balance, goal);
        List<RecommendationCandidate> selected = rank(bestPerRestaurant, balance, goal).stream()
            .limit(MAX_RECOMMENDATIONS)
            .collect(Collectors.toList());

        RecommendationExplanations explanations = assistantService.explainRestaurantRecommendations(balance, goal, message, selected);

        RestaurantRecommendationResponse response = new RestaurantRecommendationResponse();
        response.setPoints(balance);
        response.setGoal(goal);
        response.setMessage(explanations.getOverallMessage());
        response.setAiGenerated(explanations.isAiGenerated());
        response.setNotice(explanations.getNotice());
        response.setRecommendations(selected.stream()
            .map(c -> toCard(c, restaurants.get(c.restaurantId()), explanations, balance))
            .collect(Collectors.toList()));
        return response;
    }

    // ------------------------------------------------------------- candidates

    private List<RecommendationCandidate> buildEligibleCandidates(int balance, Map<String, Restaurant> restaurants,
                                                                    String preferredCuisine) {
        LocalDateTime now = LocalDateTime.now();
        List<Reward> restaurantRewards = rewardRepository.findByActiveTrueAndRestaurantIdNotNull();

        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (Reward reward : restaurantRewards) {
            if (reward.getExpiryDate() != null && reward.getExpiryDate().isBefore(now)) {
                continue; // expired: never recommend
            }
            if (reward.getStock() != null && reward.getStock() <= 0) {
                continue; // out of stock: never recommend
            }
            Restaurant restaurant = restaurants.get(reward.getRestaurantId());
            if (restaurant == null) {
                continue; // dangling reference: never show an unverifiable restaurant
            }
            if (Boolean.FALSE.equals(restaurant.getIsOpen())) {
                continue; // closed restaurants are not useful recommendations
            }
            int pointsCost = reward.getPointsCost() != null ? reward.getPointsCost() : Integer.MAX_VALUE;
            boolean affordable = pointsCost <= balance;
            int pointsMissing = Math.max(0, pointsCost - balance);
            boolean matchesCuisine = preferredCuisine != null && restaurant.getCuisine() != null
                && preferredCuisine.equalsIgnoreCase(restaurant.getCuisine());

            candidates.add(new RecommendationCandidate(
                restaurant.getId(), restaurant.getName(), restaurant.getCuisine(),
                restaurant.getPriceRange() != null ? restaurant.getPriceRange().name() : null,
                restaurant.getRating(), reward.getId(), reward.getName(),
                reward.getCategory() != null ? reward.getCategory().name() : null,
                pointsCost, affordable, pointsMissing, reward.getExpiryDate(),
                reward.getTermsAndConditions(), matchesCuisine));
        }
        return candidates;
    }

    /** At most one reward per restaurant, so the list shown to the customer stays diverse. */
    private List<RecommendationCandidate> keepBestRewardPerRestaurant(List<RecommendationCandidate> candidates,
                                                                       int balance, String goal) {
        Map<String, RecommendationCandidate> bestByRestaurant = new LinkedHashMap<>();
        for (RecommendationCandidate c : rank(candidates, balance, goal)) {
            bestByRestaurant.putIfAbsent(c.restaurantId(), c);
        }
        return new ArrayList<>(bestByRestaurant.values());
    }

    // ------------------------------------------------------------------ ranking

    private List<RecommendationCandidate> rank(List<RecommendationCandidate> candidates, int balance, String goal) {
        Comparator<RecommendationCandidate> comparator = GOAL_SAVE_FOR_PREMIUM.equals(goal)
            ? savingForPremiumComparator(balance)
            : spendNowComparator();
        return candidates.stream().sorted(comparator).collect(Collectors.toList());
    }

    /** Spend-now (default): affordable rewards first (closest to using the whole balance), then
     *  the cheapest-to-unlock non-affordable ones; cuisine match and rating break ties. */
    private Comparator<RecommendationCandidate> spendNowComparator() {
        return Comparator
            .comparing((RecommendationCandidate c) -> !c.affordable()) // false (affordable) sorts first
            .thenComparing(c -> c.affordable() ? -c.pointsCost() : c.pointsMissing()) // use most points, or need fewest
            .thenComparing((RecommendationCandidate c) -> !c.matchesPreferredCuisine())
            .thenComparing(c -> -ratingOrZero(c));
    }

    /** Saving-for-premium: the highest-value rewards reachable in the near future come first. */
    private Comparator<RecommendationCandidate> savingForPremiumComparator(int balance) {
        double reach = balance * SAVE_FOR_PREMIUM_REACH_FACTOR;
        return Comparator
            .comparing((RecommendationCandidate c) -> !(c.affordable() || c.pointsCost() <= reach))
            .thenComparing((RecommendationCandidate c) -> -c.pointsCost())
            .thenComparing((RecommendationCandidate c) -> !c.matchesPreferredCuisine())
            .thenComparing(c -> -ratingOrZero(c));
    }

    private double ratingOrZero(RecommendationCandidate c) {
        return c.rating() != null ? c.rating() : 0.0;
    }

    // -------------------------------------------------------------- preferences

    /** Most frequent cuisine across the customer's own reservations and reviews, or null if none. */
    private String inferPreferredCuisine(String userId, Map<String, Restaurant> restaurants) {
        Map<String, Long> counts = new HashMap<>();
        for (Reservation r : reservationRepository.findByUserId(userId)) {
            addCuisineCount(counts, restaurants.get(r.getRestaurantId()));
        }
        for (Review r : reviewRepository.findByUserId(userId)) {
            if (r.getRating() != null && r.getRating() >= 4.0) {
                addCuisineCount(counts, restaurants.get(r.getRestaurantId()));
            }
        }
        return counts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private void addCuisineCount(Map<String, Long> counts, Restaurant restaurant) {
        if (restaurant != null && restaurant.getCuisine() != null && !restaurant.getCuisine().isBlank()) {
            counts.merge(restaurant.getCuisine(), 1L, Long::sum);
        }
    }

    private Map<String, Restaurant> restaurantsById() {
        return restaurantRepository.findAll().stream()
            .collect(Collectors.toMap(Restaurant::getId, Function.identity(), (a, b) -> a));
    }

    // ------------------------------------------------------------------- mapping

    private RestaurantRecommendationCard toCard(RecommendationCandidate c, Restaurant restaurant,
                                                 RecommendationExplanations explanations, int balance) {
        RestaurantRecommendationCard card = new RestaurantRecommendationCard();
        card.setRestaurantId(c.restaurantId());
        card.setRestaurantName(c.restaurantName());
        card.setRestaurantImage(restaurant != null ? restaurant.getImage() : null);
        card.setCuisine(c.cuisine());
        card.setPriceRange(c.priceRange());
        card.setRating(c.rating());
        card.setRewardId(c.rewardId());
        card.setRewardName(c.rewardName());
        card.setRewardCategory(c.rewardCategory());
        card.setPointsCost(c.pointsCost());
        card.setAffordable(c.affordable());
        card.setPointsMissing(c.pointsMissing());
        card.setPointsRemainingAfterRedemption(c.affordable() ? Math.max(0, balance - c.pointsCost()) : null);
        card.setRewardExpiryDate(c.expiryDate());
        card.setRewardTerms(c.termsAndConditions());
        card.setReason(explanations.reasonFor(c));
        card.setAction(c.affordable() ? "VIEW_REWARD" : "VIEW_RESTAURANT");
        return card;
    }
}
