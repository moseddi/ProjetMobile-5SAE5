package com.hangout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.model.HangOut;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.HangOutRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration test: the whole Spring context on top of an embedded MongoDB,
 * exercised through HTTP. This is the "does it actually work end to end?" layer —
 * it wires controller + service + repository + real database together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HangOut - end-to-end integration")
class HangOutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private HangOutRepository hangOutRepository;

    private String creatorId;
    private String joinerId;
    private String restaurantId;

    @BeforeEach
    void setUp() {
        hangOutRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();

        creatorId = userRepository.save(user("creator@test.com", "Creator")).getId();
        joinerId = userRepository.save(user("joiner@test.com", "Joiner")).getId();
        restaurantId = restaurantRepository.save(restaurant("Bella Italia", "Italian")).getId();
    }

    @Test
    @DisplayName("create -> join -> duplicate join fails -> leave -> cancel, through HTTP")
    void fullLifecycle() throws Exception {
        // 1. creator creates the hangout
        String created = mockMvc.perform(post("/api/hangouts")
                .param("userId", creatorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("Italian Dinner", "2999-01-01T19:30:00", 4)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Italian Dinner"))
            .andExpect(jsonPath("$.restaurantName").value("Bella Italia"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.joinedCount").value(1))
            .andReturn().getResponse().getContentAsString();

        String hangOutId = objectMapper.readTree(created).get("id").asText();

        // 2. another user joins
        mockMvc.perform(post("/api/hangouts/" + hangOutId + "/join").param("userId", joinerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.joinedCount").value(2));

        // 3. joining twice is rejected
        mockMvc.perform(post("/api/hangouts/" + hangOutId + "/join").param("userId", joinerId))
            .andExpect(status().isBadRequest());

        // 4. the joiner leaves
        mockMvc.perform(post("/api/hangouts/" + hangOutId + "/leave").param("userId", joinerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.joinedCount").value(1));

        // 5. a non-organizer cannot cancel
        mockMvc.perform(delete("/api/hangouts/" + hangOutId).param("userId", joinerId))
            .andExpect(status().isBadRequest());

        // 6. the organizer cancels
        mockMvc.perform(delete("/api/hangouts/" + hangOutId).param("userId", creatorId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/hangouts/" + hangOutId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("creating a hangout with invalid data returns 400 and nothing is stored")
    void invalidCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/hangouts")
                .param("userId", creatorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantId\":\"\",\"title\":\"\",\"plannedDateTime\":\"2000-01-01T10:00\",\"maxParticipants\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.title").exists());

        org.assertj.core.api.Assertions.assertThat(hangOutRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /api/hangouts?upcoming=true only returns open/full hangouts from the database")
    void upcomingFilterUsesDatabase() throws Exception {
        mockMvc.perform(post("/api/hangouts")
                .param("userId", creatorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("Open One", "2999-01-01T19:30:00", 4)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/hangouts").param("upcoming", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Open One"));
    }

    @Test
    @DisplayName("the AI assistant integrates with the restaurant catalogue and falls back offline")
    void aiAssistantUsesCatalogueAndFallsBack() throws Exception {
        // ai.enabled=false in the test profile => no network, rule-based fallback
        mockMvc.perform(post("/api/ai/hangout-assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"we want an italian dinner\",\"partySize\":4}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantName").value("Bella Italia"))
            .andExpect(jsonPath("$.aiGenerated").value(false))
            .andExpect(jsonPath("$.provider").value("fallback"));
    }

    // helpers

    private String createBody(String title, String plannedDateTime, int maxParticipants) {
        return String.format(
            "{\"restaurantId\":\"%s\",\"title\":\"%s\",\"description\":\"desc\","
                + "\"plannedDateTime\":\"%s\",\"maxParticipants\":%d,\"aiGenerated\":true}",
            restaurantId, title, plannedDateTime, maxParticipants);
    }

    private User user(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword("irrelevant");
        return user;
    }

    private Restaurant restaurant(String name, String cuisine) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setCuisine(cuisine);
        restaurant.setPriceRange(Restaurant.PriceRange.MODERATE);
        restaurant.setRating(4.5);
        restaurant.setDescription("A test restaurant");
        return restaurant;
    }
}
