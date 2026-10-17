package com.hangout.service;

import com.hangout.dto.loyalty.RecommendationExplanations;
import com.hangout.dto.loyalty.RestaurantRecommendationResponse;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.Reward;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the deterministic candidate selection behind the "find me the best
 * restaurants for my points" feature. The LLM is only ever asked to explain a shortlist
 * this service already decided — these tests prove the shortlist itself is correct and
 * that the feature never touches the points ledger.
 */
@DisplayName("RestaurantRecommendationService - deterministic eligibility and ranking (unit)")
class RestaurantRecommendationServiceTest {

    private LoyaltyService loyaltyService;
    private LoyaltyAssistantService assistantService;
    private RewardRepository rewardRepository;
    private RestaurantRepository restaurantRepository;
    private ReservationRepository reservationRepository;
    private ReviewRepository reviewRepository;
    private RestaurantRecommendationService service;

    @BeforeEach
    void setUp() {
        loyaltyService = mock(LoyaltyService.class);
        assistantService = mock(LoyaltyAssistantService.class);
        rewardRepository = mock(RewardRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        reviewRepository = mock(ReviewRepository.class);

        service = new RestaurantRecommendationService(loyaltyService, assistantService,
            rewardRepository, restaurantRepository, reservationRepository, reviewRepository);

        when(reservationRepository.findByUserId(anyString())).thenReturn(List.of());
        when(reviewRepository.findByUserId(anyString())).thenReturn(List.of());

        // Explanations are not under test here: echo back a deterministic, obviously-fallback response
        when(assistantService.explainRestaurantRecommendations(anyInt(), any(), any(), anyList()))
            .thenAnswer(inv -> {
                RecommendationExplanations result = new RecommendationExplanations();
                result.setOverallMessage("stub");
                result.setAiGenerated(false);
                return result;
            });
    }

    private Restaurant restaurant(String id, String name, String cuisine, boolean open, Double rating) {
        Restaurant r = new Restaurant();
        r.setId(id);
        r.setName(name);
        r.setCuisine(cuisine);
        r.setImage("https://img/" + id);
        r.setIsOpen(open);
        r.setRating(rating);
        r.setPriceRange(Restaurant.PriceRange.MODERATE);
        return r;
    }

    private Reward reward(String id, String restaurantId, int pointsCost, LocalDateTime expiry, Integer stock) {
        Reward r = new Reward();
        r.setId(id);
        r.setName("Reward " + id);
        r.setRestaurantId(restaurantId);
        r.setPointsCost(pointsCost);
        r.setActive(true);
        r.setCategory(Reward.RewardCategory.DISCOUNT);
        r.setExpiryDate(expiry);
        r.setStock(stock);
        return r;
    }

    @Test
    @DisplayName("uses the real ledger balance, never a client-supplied one")
    void recommend_usesRealBalance() {
        when(loyaltyService.currentBalance("u1")).thenReturn(800);
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getPoints()).isEqualTo(800);
        verify(loyaltyService).currentBalance("u1");
    }

    @Test
    @DisplayName("never mutates the points ledger just by asking for recommendations")
    void recommend_neverTouchesLedger() {
        when(loyaltyService.currentBalance("u1")).thenReturn(500);
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());

        service.recommend("u1", null, null);

        verify(loyaltyService, never()).creditPoints(any(), anyInt(), any(), any(), any());
        verify(loyaltyService, never()).debitPoints(any(), anyInt(), any(), any(), any());
        verify(loyaltyService, never()).redeemReward(any(), any());
    }

    @Test
    @DisplayName("empty reward catalog produces an empty, non-fabricated recommendation list")
    void recommend_emptyRewardCatalog_returnsEmptyList() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of());
        when(restaurantRepository.findAll()).thenReturn(List.of());

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("filters out an expired reward")
    void recommend_filtersExpiredReward() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, LocalDateTime.now().minusDays(1), null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("filters out an out-of-stock reward")
    void recommend_filtersOutOfStockReward() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, null, 0)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("filters out a reward at a closed restaurant")
    void recommend_filtersClosedRestaurant() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant closed = restaurant("r1", "Closed Place", "French", false, 4.0);
        when(restaurantRepository.findAll()).thenReturn(List.of(closed));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, null, null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("filters out a reward pointing at a restaurant that no longer exists")
    void recommend_filtersDanglingRestaurantReference() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        when(restaurantRepository.findAll()).thenReturn(List.of());
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "ghost-restaurant", 300, null, null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    @DisplayName("with sufficient points, marks the reward affordable and computes remaining points from the real balance")
    void recommend_sufficientPoints_marksAffordable() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, null, null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).hasSize(1);
        var card = response.getRecommendations().get(0);
        assertThat(card.isAffordable()).isTrue();
        assertThat(card.getPointsMissing()).isZero();
        assertThat(card.getPointsRemainingAfterRedemption()).isEqualTo(700);
    }

    @Test
    @DisplayName("with insufficient points, marks the reward unaffordable and reports exactly how many points are missing")
    void recommend_insufficientPoints_reportsPointsMissing() {
        when(loyaltyService.currentBalance("u1")).thenReturn(100);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, null, null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).hasSize(1);
        var card = response.getRecommendations().get(0);
        assertThat(card.isAffordable()).isFalse();
        assertThat(card.getPointsMissing()).isEqualTo(200);
        assertThat(card.getPointsRemainingAfterRedemption()).isNull();
    }

    @Test
    @DisplayName("keeps at most one reward per restaurant so the list stays diverse")
    void recommend_keepsOnlyBestRewardPerRestaurant() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of(
            reward("cheap", "r1", 200, null, null),
            reward("expensive", "r1", 900, null, null)
        ));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    @DisplayName("without reservation or review history, still works and infers no cuisine preference")
    void recommend_missingPreferences_stillWorks() {
        when(loyaltyService.currentBalance("u1")).thenReturn(500);
        when(reservationRepository.findByUserId("u1")).thenReturn(List.of());
        when(reviewRepository.findByUserId("u1")).thenReturn(List.of());
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 200, null, null)));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    @DisplayName("infers a cuisine preference from past reservations and ranks a matching restaurant first")
    void recommend_inferredCuisinePreference_ranksMatchFirst() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant italian = restaurant("r-it", "Bella Italia", "Italian", true, 4.0);
        Restaurant french = restaurant("r-fr", "Le Gourmet", "French", true, 4.0);
        when(restaurantRepository.findAll()).thenReturn(List.of(italian, french));

        Reservation past = new Reservation();
        past.setUserId("u1");
        past.setRestaurantId("r-it");
        when(reservationRepository.findByUserId("u1")).thenReturn(List.of(past, past, past));

        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of(
            reward("rw-it", "r-it", 500, null, null),
            reward("rw-fr", "r-fr", 500, null, null)
        ));

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.getRecommendations()).hasSize(2);
        assertThat(response.getRecommendations().get(0).getRestaurantId()).isEqualTo("r-it");
    }

    @Test
    @DisplayName("passes only the already-filtered, deterministic candidates to the AI explainer")
    void recommend_passesOnlyVerifiedCandidatesToAssistant() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull()).thenReturn(List.of(
            reward("rw1", "r1", 300, null, null),
            reward("expired", "r1", 100, LocalDateTime.now().minusDays(1), null)
        ));

        service.recommend("u1", null, null);

        verify(assistantService).explainRestaurantRecommendations(eq(1000), isNull(), isNull(), argThat(candidates ->
            candidates.size() == 1 && candidates.get(0).rewardId().equals("rw1")));
    }

    @Test
    @DisplayName("forwards the AI explainer's fallback flag and notice unchanged when it falls back")
    void recommend_surfacesFallbackNotice() {
        when(loyaltyService.currentBalance("u1")).thenReturn(1000);
        Restaurant r1 = restaurant("r1", "Le Gourmet", "French", true, 4.5);
        when(restaurantRepository.findAll()).thenReturn(List.of(r1));
        when(rewardRepository.findByActiveTrueAndRestaurantIdNotNull())
            .thenReturn(List.of(reward("rw1", "r1", 300, null, null)));

        RecommendationExplanations fallback = new RecommendationExplanations();
        fallback.setOverallMessage("Explications basees sur des regles.");
        fallback.setAiGenerated(false);
        fallback.setNotice("Aucune cle API IA n'est configuree.");
        when(assistantService.explainRestaurantRecommendations(anyInt(), any(), any(), anyList())).thenReturn(fallback);

        RestaurantRecommendationResponse response = service.recommend("u1", null, null);

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getNotice()).isEqualTo("Aucune cle API IA n'est configuree.");
    }
}
