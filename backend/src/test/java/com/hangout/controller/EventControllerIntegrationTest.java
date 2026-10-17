package com.hangout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.ai.EventDescriptionService;
import com.hangout.ai.GenerateDescriptionRequest;
import com.hangout.ai.GeneratedDescription;
import com.hangout.ai.SimpleRateLimiter;
import com.hangout.model.Event;
import com.hangout.service.EventService;
import com.hangout.service.JwtService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ==========================================================================
 * 🎟️ MODULE : ÉVÉNEMENTS (TRAVAIL DE MA COLLÈGUE / FRIEND'S MODULE)
 * Auteur : Binôme Événements
 * Rôle : Contrôleur REST Événements & Génération Description IA
 * ==========================================================================
 * Tests d'Intégration / Fonctionnels (MockMvc) pour EventController.
 */
@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private EventDescriptionService descriptionService;

    @MockBean
    private SimpleRateLimiter rateLimiter;

    // Not used by these tests, but SecurityConfig's JwtAuthFilter needs it to build in this slice
    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/events -> 200 OK with list of events")
    void shouldGetAllEvents() throws Exception {
        Event event = new Event();
        event.setId("evt-1");
        event.setTitle("Jazz & Tapas");

        when(eventService.getAll()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("evt-1"))
                .andExpect(jsonPath("$[0].title").value("Jazz & Tapas"));
    }

    @Test
    @DisplayName("POST /api/events -> 201 CREATED with new Event")
    void shouldCreateEvent() throws Exception {
        Event event = new Event();
        event.setTitle("Brunch Acoustique");
        event.setPrice(35.0);

        when(eventService.create(any(Event.class))).thenReturn(event);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Brunch Acoustique"));
    }

    @Test
    @DisplayName("POST /api/events/generate-description -> 200 OK with AI generated copy")
    void shouldGenerateAiDescription() throws Exception {
        GenerateDescriptionRequest request = new GenerateDescriptionRequest(
                "Soirée Tapas", "2026-11-15", "Espagnole", "Paella royale", "Festive", 40, "festif et dynamique"
        );

        GeneratedDescription aiResponse = new GeneratedDescription(
                "Noche Latina & Saveurs d'Espagne",
                "Rejoignez-nous pour une soirée ensoleillée avec paella et musique live !",
                List.of("#Tapas", "#HangOut", "#Fiesta", "#GoodVibes"),
                "Réservez votre table dès maintenant !"
        );

        when(descriptionService.generate(any(GenerateDescriptionRequest.class))).thenReturn(aiResponse);

        mockMvc.perform(post("/api/events/generate-description")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Noche Latina & Saveurs d'Espagne"))
                .andExpect(jsonPath("$.hashtags").isArray())
                .andExpect(jsonPath("$.appel_action").isNotEmpty());
    }
}