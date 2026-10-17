package com.hangout.service;

import com.hangout.dto.AiHangoutRequest;
import com.hangout.dto.AiHangoutResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the rule-based part of {@link AiService} (no HTTP involved).
 *
 * <p>When no API key is configured the service must produce a deterministic
 * recommendation: this is exactly the "safe fallback" the assignment requires,
 * so it is worth testing thoroughly.
 */
@DisplayName("AiService - rule-based fallback (unit)")
class AiServiceTest {

    private RestaurantRepository restaurantRepository;
    private AiService aiService;
    private List<Restaurant> catalogue;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        aiService = new AiService(restaurantRepository);

        ReflectionTestUtils.setField(aiService, "aiEnabled", true);
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "");
        ReflectionTestUtils.setField(aiService, "geminiModel", "gemini-test");
        ReflectionTestUtils.setField(aiService, "geminiModels", "gemini-test");
        ReflectionTestUtils.setField(aiService, "geminiUrl", "http://localhost:1/v1beta/models");
        ReflectionTestUtils.setField(aiService, "timeoutMs", 1000L);

        catalogue = List.of(
            restaurant("r-it", "Bella Italia", "Italian", Restaurant.PriceRange.MODERATE, 4.7, List.of("Vegetarian", "Terrace")),
            restaurant("r-jp", "Sushi Paradise", "Japanese", Restaurant.PriceRange.EXPENSIVE, 4.5, List.of("Sake")),
            restaurant("r-fr", "Le Gourmet", "French", Restaurant.PriceRange.LUXURY, 4.8, List.of("Michelin")),
            restaurant("r-bg", "Cheap Eats", "American", Restaurant.PriceRange.BUDGET, 3.9, List.of("Takeaway"))
        );
        when(restaurantRepository.findAll()).thenReturn(catalogue);
    }

    @Test
    @DisplayName("without an API key returns a rule-based suggestion, never calls the network")
    void withoutApiKey_usesRuleBasedFallback() {
        AiHangoutResponse response = aiService.suggestHangout(request("dinner with friends", 4, null));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
        assertThat(response.getNotice()).contains("No AI API key");
        assertThat(response.getRestaurantId()).isNotBlank();
        assertThat(response.getTitle()).isNotBlank();
        assertThat(response.getKeywords()).isNotEmpty();
    }

    @Test
    @DisplayName("matches an Italian restaurant from an English keyword")
    void fallbackPicksRestaurantByCuisineKeyword() {
        AiHangoutResponse response = aiService.suggestHangout(request("italian dinner please", 4, null));
        assertThat(response.getRestaurantId()).isEqualTo("r-it");
    }

    @Test
    @DisplayName("matches an Italian restaurant from a French synonym (pizza/pasta)")
    void fallbackUnderstandsFrenchSynonyms() {
        AiHangoutResponse response = aiService.suggestHangout(
            request("on veut manger des pizzas entre amis", 5, null));
        assertThat(response.getRestaurantId()).isEqualTo("r-it");
    }

    @Test
    @DisplayName("matches the restaurant whose name is mentioned")
    void fallbackMatchesRestaurantByName() {
        AiHangoutResponse response = aiService.suggestHangout(
            request("let's meet at Sushi Paradise tonight", 3, null));
        assertThat(response.getRestaurantId()).isEqualTo("r-jp");
    }

    @Test
    @DisplayName("matches a luxury restaurant when the prompt mentions 'luxury'")
    void fallbackMatchesByBudget() {
        AiHangoutResponse response = aiService.suggestHangout(
            request("we want a luxury dining experience", 2, null));
        assertThat(response.getRestaurantId()).isEqualTo("r-fr");
    }

    @Test
    @DisplayName("empty catalogue raises a BadRequestException (never a 500)")
    void emptyCatalogueRaisesBadRequest() {
        when(restaurantRepository.findAll()).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> aiService.suggestHangout(request("anything", 2, null)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("No restaurants");
    }

    @Test
    @DisplayName("sanitizeDateTime replaces a past date with a future default")
    void sanitizeDateTimeRejectsPastDate() {
        String result = ReflectionTestUtils.invokeMethod(aiService, "sanitizeDateTime", "2020-01-01T10:00");
        assertThat(result).isNotNull();
        assertThat(LocalDateTime.parse(result)).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("sanitizeDateTime keeps a valid future date")
    void sanitizeDateTimeKeepsFutureDate() {
        String result = ReflectionTestUtils.invokeMethod(aiService, "sanitizeDateTime", "2999-01-01T12:00");
        assertThat(result).isEqualTo("2999-01-01T12:00");
    }

    @Test
    @DisplayName("sanitizeDateTime falls back to a future default for unparsable input")
    void sanitizeDateTimeHandlesGarbage() {
        String result = ReflectionTestUtils.invokeMethod(aiService, "sanitizeDateTime", "not-a-date");
        assertThat(result).isNotNull();
        assertThat(LocalDateTime.parse(result)).isAfter(LocalDateTime.now());
    }

    private AiHangoutRequest request(String prompt, Integer partySize, String budget) {
        AiHangoutRequest request = new AiHangoutRequest();
        request.setPrompt(prompt);
        request.setPartySize(partySize);
        request.setBudget(budget);
        return request;
    }

    private Restaurant restaurant(String id, String name, String cuisine,
                                  Restaurant.PriceRange priceRange, double rating, List<String> features) {
        Restaurant r = new Restaurant();
        r.setId(id);
        r.setName(name);
        r.setCuisine(cuisine);
        r.setPriceRange(priceRange);
        r.setRating(rating);
        r.setFeatures(features);
        r.setDescription("A lovely place");
        return r;
    }
}
