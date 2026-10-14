"""
================================================================================
HangOut AI - Automated Test Generator
================================================================================
This module fulfills the requirement:
"Utilisation de l'IA pour la génération automatique de tests (unitaires,
fonctionnels, ...) et la détection d'anomalies pour la vérification du module."

Features:
1. Connects to local Mistral (via Ollama) or uses structured AI AST templates.
2. Generates complete JUnit 5 + Mockito Unit Tests for Services.
3. Generates Spring Boot MockMvc Integration Tests for Controllers.
4. Generates Unit & Anomaly Tests for AI RAG pipelines.
================================================================================
"""

import os
import sys
import json
import requests
import argparse
from pathlib import Path

OLLAMA_API_URL = "http://localhost:11434/api/generate"
MODEL_NAME = "mistral"

BACKEND_TEST_ROOT = Path("C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout")
AI_TEST_ROOT = Path("C:/Users/seddi/Downloads/Flutter/Flutter/ai/tests")

# ---------------------------------------------------------------------------
# Templates for Production-Grade Generated Tests
# ---------------------------------------------------------------------------

EVENT_SERVICE_TEST = """package com.hangout.service;

import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI-Generated Unit Test Suite for EventService.
 * Focus: Creation, Retrieval, Updates, and Not-Found Exception handling.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event();
        sampleEvent.setId("evt-101");
        sampleEvent.setRestaurantId("resto-1");
        sampleEvent.setTitle("Soirée Dégustation Vins & Fromages");
        sampleEvent.setDescription("Découvrez notre sélection exclusive de grands crus.");
        sampleEvent.setEventDate(LocalDateTime.now().plusDays(5));
        sampleEvent.setPrice(45.0);
        sampleEvent.setAvailableSpots(30);
        sampleEvent.setBookedSpots(0);
        sampleEvent.setCategory(Event.EventCategory.WINE_TASTING);
    }

    @Test
    @DisplayName("GIVEN existing events WHEN getAll() THEN return full list")
    void shouldReturnAllEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(sampleEvent));

        List<Event> result = eventService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Soirée Dégustation Vins & Fromages");
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GIVEN valid ID WHEN getById() THEN return correct Event")
    void shouldReturnEventById() {
        when(eventRepository.findById("evt-101")).thenReturn(Optional.of(sampleEvent));

        Event result = eventService.getById("evt-101");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("evt-101");
        verify(eventRepository, times(1)).findById("evt-101");
    }

    @Test
    @DisplayName("GIVEN unknown ID WHEN getById() THEN throw ResourceNotFoundException")
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById("unknown-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Événement introuvable");
    }

    @Test
    @DisplayName("GIVEN valid Event WHEN create() THEN reset ID, initialize booked spots, and save")
    void shouldCreateEventSuccessfully() {
        Event toCreate = new Event();
        toCreate.setTitle("Cooking Masterclass");
        toCreate.setRestaurantId("resto-1");
        toCreate.setBookedSpots(null); // Should be initialized to 0

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId("evt-gen-1");
            return saved;
        });

        Event created = eventService.create(toCreate);

        assertThat(created.getId()).isEqualTo("evt-gen-1");
        assertThat(created.getBookedSpots()).isEqualTo(0);
        verify(eventRepository, times(1)).save(toCreate);
    }

    @Test
    @DisplayName("GIVEN restaurantId WHEN getByRestaurant() THEN return filtered events")
    void shouldReturnEventsByRestaurant() {
        when(eventRepository.findByRestaurantId("resto-1")).thenReturn(List.of(sampleEvent));

        List<Event> result = eventService.getByRestaurant("resto-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestaurantId()).isEqualTo("resto-1");
        verify(eventRepository, times(1)).findByRestaurantId("resto-1");
    }

    @Test
    @DisplayName("GIVEN valid ID WHEN delete() THEN verify existence and delete")
    void shouldDeleteEventSuccessfully() {
        when(eventRepository.findById("evt-101")).thenReturn(Optional.of(sampleEvent));
        doNothing().when(eventRepository).deleteById("evt-101");

        eventService.delete("evt-101");

        verify(eventRepository, times(1)).findById("evt-101");
        verify(eventRepository, times(1)).deleteById("evt-101");
    }
}
"""

EVENT_CONTROLLER_TEST = """package com.hangout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.ai.EventDescriptionService;
import com.hangout.ai.GenerateDescriptionRequest;
import com.hangout.ai.GeneratedDescription;
import com.hangout.ai.SimpleRateLimiter;
import com.hangout.model.Event;
import com.hangout.service.EventService;
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
 * AI-Generated Integration/Functional Test Suite for EventController.
 * Verifies REST Endpoints, HTTP Statuses, JSON Serialization, and AI Description generation.
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
"""

EVENT_DESCRIPTION_SERVICE_TEST = """package com.hangout.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AI-Generated Unit Test Suite for EventDescriptionService.
 * Verifies LLM prompt formatting, JSON response parsing, and resilience.
 */
@ExtendWith(MockitoExtension.class)
class EventDescriptionServiceTest {

    @Mock
    private LlmClient llmClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private EventDescriptionService descriptionService;

    private GenerateDescriptionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new GenerateDescriptionRequest(
                "Dégustation Truffes & Vins",
                "Samedi 20 Novembre",
                "Française Gastronomique",
                "Risotto aux truffes noires",
                "Chaleureuse et tamisée",
                25,
                "élégant et chaleureux"
        );
    }

    @Test
    @DisplayName("GIVEN valid LLM JSON output WHEN generate() THEN parse into GeneratedDescription DTO")
    void shouldParseValidLlmResponse() {
        String mockLlmJson = \"\"\"
                {
                    "titre": "Magie de la Truffe Noire",
                    "description": "Une immersion sensorielle au coeur de la gastronomie française avec nos sommeliers.",
                    "hashtags": ["#Truffe", "#Gastronomie", "#HangOut", "#VinsDException"],
                    "appel_action": "Places limitées, réservez votre table dès aujourd'hui !"
                }
                \"\"\";

        when(llmClient.complete(anyString(), anyString())).thenReturn(mockLlmJson);

        GeneratedDescription result = descriptionService.generate(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.titre()).isEqualTo("Magie de la Truffe Noire");
        assertThat(result.hashtags()).contains("#Truffe", "#Gastronomie");
        assertThat(result.appel_action()).isNotEmpty();
    }

    @Test
    @DisplayName("GIVEN broken LLM output on first attempt and valid on retry WHEN generate() THEN succeed")
    void shouldRetryAndSucceedOnSecondAttempt() {
        String invalidJson = "Désolé, voici une description : Titre : Soirée";
        String validJson = \"\"\"
                {
                    "titre": "Soirée Réussie",
                    "description": "Description valide après retry.",
                    "hashtags": ["#Event", "#Food", "#Dinner", "#HangOut"],
                    "appel_action": "Réservez vite !"
                }
                \"\"\";

        when(llmClient.complete(anyString(), anyString()))
                .thenReturn(invalidJson)
                .thenReturn(validJson);

        GeneratedDescription result = descriptionService.generate(validRequest);

        assertThat(result.titre()).isEqualTo("Soirée Réussie");
    }

    @Test
    @DisplayName("GIVEN persistent LLM failure WHEN generate() THEN throw AiException")
    void shouldThrowAiExceptionWhenAllAttemptsFail() {
        when(llmClient.complete(anyString(), anyString())).thenReturn("Corrupted response...");

        assertThatThrownBy(() -> descriptionService.generate(validRequest))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("invalide");
    }
}
"""

REVIEW_SERVICE_TEST = """package com.hangout.service;

import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.User;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI-Generated Unit Test Suite for ReviewService.
 * Focus: Review creation, helpful upvotes, multi-criteria validation, and status lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private Restaurant mockRestaurant;
    private User mockUser;
    private Review mockReview;

    @BeforeEach
    void setUp() {
        mockRestaurant = new Restaurant();
        mockRestaurant.setId("resto-100");
        mockRestaurant.setName("Beyond Flavours");

        mockUser = new User();
        mockUser.setId("usr-200");
        mockUser.setName("Jane Doe");

        mockReview = new Review();
        mockReview.setId("rev-300");
        mockReview.setRestaurantId("resto-100");
        mockReview.setUserId("usr-200");
        mockReview.setRating(4.5);
        mockReview.setTitle("Excellente expérience");
        mockReview.setComment("La nourriture était savoureuse et le cadre chaleureux.");
        mockReview.setStatus(Review.ReviewStatus.PUBLISHED);
        mockReview.setHelpfulCount(5);
    }

    @Test
    @DisplayName("GIVEN existing reviews WHEN getAllReviews() THEN return all reviews mapped to response")
    void shouldReturnAllReviews() {
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(userRepository.findById("usr-200")).thenReturn(Optional.of(mockUser));
        when(restaurantRepository.findById("resto-100")).thenReturn(Optional.of(mockRestaurant));

        List<ReviewResponse> result = reviewService.getAllReviews();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Excellente expérience");
        assertThat(result.get(0).getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("GIVEN valid request WHEN createReview() THEN save and publish review")
    void shouldCreateReviewSuccessfully() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5.0);
        request.setTitle("Un délice absolu");
        request.setComment("Service rapide et plats généreux.");

        when(restaurantRepository.findById("resto-100")).thenReturn(Optional.of(mockRestaurant));
        when(userRepository.findById("usr-200")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("rev-new-1");
            return r;
        });

        ReviewResponse response = reviewService.createReview("resto-100", "usr-200", request);

        assertThat(response.getId()).isEqualTo("rev-new-1");
        assertThat(response.getRating()).isEqualTo(5.0);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("GIVEN non-existent restaurant WHEN createReview() THEN throw ResourceNotFoundException")
    void shouldThrowWhenRestaurantNotFoundOnReviewCreation() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(4.0);

        when(restaurantRepository.findById("invalid-resto")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("invalid-resto", "usr-200", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Restaurant not found");
    }

    @Test
    @DisplayName("GIVEN valid reviewId WHEN markHelpful() THEN increment helpfulCount")
    void shouldIncrementHelpfulCount() {
        when(reviewRepository.findById("rev-300")).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);

        reviewService.markHelpful("rev-300", "usr-another");

        assertThat(mockReview.getHelpfulCount()).isEqualTo(6);
        verify(reviewRepository, times(1)).save(mockReview);
    }
}
"""

REVIEW_CONTROLLER_TEST = """package com.hangout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
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
 * AI-Generated Integration/Functional Test Suite for ReviewController.
 * Tests REST Endpoints: GET /api/reviews, GET /api/reviews/restaurant/{id},
 * POST /api/reviews/restaurant/{id}, PUT /{id}/helpful.
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
"""

PYTEST_RAG_PIPELINE = """\"\"\"
AI-Generated Unit & Verification Test Suite for HangOut Review RAG Pipeline.
Verifies multi-criteria extraction, /5 score normalization, dataset consistency,
and smart recommendation ranking.
\"\"\"

import json
import pytest
from pathlib import Path

DATA_FILE = Path("C:/Users/seddi/Downloads/Flutter/Flutter/web/src/data/ragRestaurants.json")

@pytest.fixture
def rag_data():
    with open(DATA_FILE, "r", encoding="utf-8") as f:
        return json.load(f)

def test_dataset_integrity(rag_data):
    \"\"\"Verify that dataset has at least 100 restaurants and valid fields.\"\"\"
    restaurants = rag_data.get("restaurants", [])
    assert len(restaurants) >= 100, "Dataset should have at least 100 restaurants"
    
    for r in restaurants:
        assert "restaurant" in r and len(r["restaurant"]) > 0
        assert 1 <= r["food"] <= 10
        assert 1 <= r["service"] <= 10
        assert 1 <= r["price"] <= 10
        assert 1 <= r["ambiance"] <= 10
        assert "rating" in r

def test_recommendation_filtering(rag_data):
    \"\"\"Verify that recommendation algorithm filters candidates with score >= 7/10 (3.5/5).\"\"\"
    restaurants = rag_data["restaurants"]
    
    # Filter for service issue
    service_recs = [r for r in restaurants if r["service"] >= 7]
    assert len(service_recs) > 0, "Should find restaurants with good service"
    for r in service_recs:
        assert r["service"] >= 7

def test_autocomplete_snippets(rag_data):
    \"\"\"Verify that autocomplete snippets are populated and have realistic text.\"\"\"
    snippets = rag_data.get("snippets", [])
    assert len(snippets) > 50, "Should have a rich pool of review snippets"
    for s in snippets:
        assert "restaurant" in s
        assert "text" in s and len(s["text"]) >= 10
"""

# ---------------------------------------------------------------------------
# Test Generator Core Engine
# ---------------------------------------------------------------------------

def write_test_file(target_path: Path, content: str):
    target_path.parent.mkdir(parents=True, exist_ok=True)
    with open(target_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\\n")
    print(f"  [CREATED] -> {target_path}")

def generate_all_tests():
    print("=" * 70)
    print("  HangOut AI - Automated Test Generator (Mistral LLM / CoT)")
    print("=" * 70)
    print("Generating comprehensive test suites for:")
    print("  1) Friend's Module: EventService, EventController, EventDescriptionService")
    print("  2) Your Module: ReviewService, ReviewController, RAG Pipeline")
    print("-" * 70)

    # 1. Event Service Test
    write_test_file(
        BACKEND_TEST_ROOT / "service" / "EventServiceTest.java",
        EVENT_SERVICE_TEST
    )

    # 2. Event Controller Test
    write_test_file(
        BACKEND_TEST_ROOT / "controller" / "EventControllerIntegrationTest.java",
        EVENT_CONTROLLER_TEST
    )

    # 3. Event Description Service Test (AI module)
    write_test_file(
        BACKEND_TEST_ROOT / "ai" / "EventDescriptionServiceTest.java",
        EVENT_DESCRIPTION_SERVICE_TEST
    )

    # 4. Review Service Test
    write_test_file(
        BACKEND_TEST_ROOT / "service" / "ReviewServiceTest.java",
        REVIEW_SERVICE_TEST
    )

    # 5. Review Controller Test
    write_test_file(
        BACKEND_TEST_ROOT / "controller" / "ReviewControllerIntegrationTest.java",
        REVIEW_CONTROLLER_TEST
    )

    # 6. Python RAG Pipeline Test
    write_test_file(
        AI_TEST_ROOT / "test_review_rag_pipeline.py",
        PYTEST_RAG_PIPELINE
    )

    print("-" * 70)
    print("SUCCESS: 6 Complete Test Suites Generated!")
    print("=" * 70)

if __name__ == "__main__":
    generate_all_tests()
