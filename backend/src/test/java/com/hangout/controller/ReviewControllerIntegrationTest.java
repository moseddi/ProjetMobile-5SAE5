package com.hangout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.service.JwtService;
import com.hangout.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ==========================================================================
 * ⭐ MODULE : AVIS & INTELLIGENCE RAG (MON TRAVAIL / MY MODULE)
 * Auteur : Responsable Module Avis & RAG
 * Rôle : Contrôleur REST des Avis (Endpoints GET, POST, PUT helpful)
 * ==========================================================================
 * Tests d'Intégration / Fonctionnels (MockMvc) pour ReviewController.
 */
@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    // Not used by these tests, but SecurityConfig's JwtAuthFilter needs it to build in this slice
    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/reviews -> 200 OK with all reviews")
    void shouldReturnAllReviews() throws Exception {
        ReviewResponse resp = new ReviewResponse();
        resp.setId("rev-1");
        resp.setTitle("Cadre magnifique");
        resp.setRating(5.0);

        when(reviewService.getAllReviews()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rev-1"))
                .andExpect(jsonPath("$[0].title").value("Cadre magnifique"));
    }

    @Test
    @DisplayName("GET /api/reviews/restaurant/{restaurantId} -> 200 OK with restaurant reviews")
    void shouldReturnRestaurantReviews() throws Exception {
        ReviewResponse resp = new ReviewResponse();
        resp.setId("rev-2");
        resp.setRestaurantId("resto-10");
        resp.setRating(4.5);

        when(reviewService.getRestaurantReviews("resto-10")).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/reviews/restaurant/resto-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantId").value("resto-10"));
    }

    @Test
    @DisplayName("POST /api/reviews/restaurant/{id}?userId={userId} -> 200 OK with created review")
    void shouldCreateReview() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setRating(4.0);
        request.setTitle("Super service");
        request.setComment("Les serveurs étaient très prévenants.");

        ReviewResponse response = new ReviewResponse();
        response.setId("rev-new-99");
        response.setTitle("Super service");
        response.setRating(4.0);

        when(reviewService.createReview(eq("resto-10"), eq("user-5"), any(ReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews/restaurant/resto-10")
                        .param("userId", "user-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rev-new-99"))
                .andExpect(jsonPath("$.rating").value(4.0));
    }
}