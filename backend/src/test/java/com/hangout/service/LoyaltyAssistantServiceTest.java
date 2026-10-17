package com.hangout.service;

import com.hangout.dto.loyalty.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the deterministic fallback path of the loyalty assistant
 * (no Gemini API key configured, so no network call is made) — this is the
 * "the app keeps working without the LLM provider" requirement.
 */
@DisplayName("LoyaltyAssistantService - rule-based fallback (unit)")
class LoyaltyAssistantServiceTest {

    private LoyaltyService loyaltyService;
    private LoyaltyAssistantService assistantService;

    @BeforeEach
    void setUp() {
        loyaltyService = mock(LoyaltyService.class);
        assistantService = new LoyaltyAssistantService(loyaltyService);

        ReflectionTestUtils.setField(assistantService, "aiEnabled", true);
        ReflectionTestUtils.setField(assistantService, "geminiApiKey", "");
        ReflectionTestUtils.setField(assistantService, "geminiModels", "gemini-test");
        ReflectionTestUtils.setField(assistantService, "geminiUrl", "http://localhost:1/v1beta/models");
        ReflectionTestUtils.setField(assistantService, "timeoutMs", 1000L);

        LoyaltyDashboardResponse dashboard = new LoyaltyDashboardResponse();
        dashboard.setPoints(250);
        dashboard.setTier("Bronze");
        dashboard.setNextTier("Silver");
        dashboard.setPointsToNextTier(250);
        dashboard.setTodaysChallenges(List.of());
        dashboard.setGames(List.of(
            new GameCatalogEntry("quiz", "Quiz du jour", "desc", "Trivia", "EASY", 3, "icon", "5 questions", "10-50", 1, 0, true),
            new GameCatalogEntry("memory", "Memory Match", "desc", "Memory", "MEDIUM", 2, "icon", "8 paires", "10-40", 3, 3, false)
        ));
        when(loyaltyService.getDashboard("u1")).thenReturn(dashboard);

        RewardDto affordable = new RewardDto();
        affordable.setId("r-cheap");
        affordable.setName("Dessert offert");
        affordable.setPointsCost(100);
        affordable.setCategory("FREE_ITEM");
        affordable.setAffordable(true);
        affordable.setPointsMissing(0);

        RewardDto locked = new RewardDto();
        locked.setId("r-expensive");
        locked.setName("Diner pour 2");
        locked.setPointsCost(3000);
        locked.setCategory("EXPERIENCE");
        locked.setAffordable(false);
        locked.setPointsMissing(2750);

        when(loyaltyService.getRewards("u1")).thenReturn(List.of(affordable, locked));
    }

    @Test
    @DisplayName("without an API key, never calls the network and flags the response as non-AI")
    void withoutApiKey_usesRuleBasedFallback() {
        AssistantChatResponse response = assistantService.chat("u1", "Quels jeux puis-je jouer aujourd'hui ?");

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getNotice()).isNotBlank();
    }

    @Test
    @DisplayName("a game-related question only recommends games that are actually available today")
    void gameQuestion_onlyRecommendsAvailableGames() {
        AssistantChatResponse response = assistantService.chat("u1", "Quels jeux puis-je jouer aujourd'hui ?");

        assertThat(response.getIntent()).isEqualTo("RECOMMEND_GAMES");
        assertThat(response.getRecommendations())
            .extracting(AssistantChatResponse.Recommendation::getId)
            .containsExactly("quiz"); // memory is not available today, must be excluded
    }

    @Test
    @DisplayName("a reward-related question recommends only rewards the user can actually afford")
    void rewardQuestion_onlyRecommendsAffordableRewards() {
        AssistantChatResponse response = assistantService.chat("u1", "Que puis-je obtenir avec mes points ?");

        assertThat(response.getIntent()).isEqualTo("RECOMMEND_REWARDS");
        assertThat(response.getRecommendations())
            .extracting(AssistantChatResponse.Recommendation::getId)
            .containsExactly("r-cheap");
    }

    @Test
    @DisplayName("a balance question states the real, verified balance")
    void balanceQuestion_statesRealBalance() {
        AssistantChatResponse response = assistantService.chat("u1", "Combien de points ai-je ?");

        assertThat(response.getMessage()).contains("250");
    }

    // ----------------------------------------------- restaurant recommendation explanations

    private RecommendationCandidate candidate(String restaurantId, String rewardId, int pointsCost, boolean affordable, int missing) {
        return new RecommendationCandidate(restaurantId, "Restaurant " + restaurantId, "French", "MODERATE", 4.2,
            rewardId, "Reward " + rewardId, "DISCOUNT", pointsCost, affordable, missing, null, null, false);
    }

    @Test
    @DisplayName("without an API key, fills every candidate with a deterministic reason and flags non-AI")
    void explainRecommendations_withoutApiKey_fillsDeterministicReasonForEveryCandidate() {
        List<RecommendationCandidate> candidates = List.of(
            candidate("r1", "rw1", 300, true, 0),
            candidate("r2", "rw2", 900, false, 400)
        );

        RecommendationExplanations result = assistantService.explainRestaurantRecommendations(500, null, null, candidates);

        assertThat(result.isAiGenerated()).isFalse();
        assertThat(result.getNotice()).isNotBlank();
        assertThat(result.reasonFor(candidates.get(0))).isNotBlank();
        assertThat(result.reasonFor(candidates.get(1))).contains("400");
    }

    @Test
    @DisplayName("an empty candidate list never calls the network and returns no notice")
    void explainRecommendations_emptyCandidates_noNoticeNoCall() {
        RecommendationExplanations result = assistantService.explainRestaurantRecommendations(500, null, null, List.of());

        assertThat(result.getReasonsByKey()).isEmpty();
        assertThat(result.getNotice()).isNull();
    }
}
