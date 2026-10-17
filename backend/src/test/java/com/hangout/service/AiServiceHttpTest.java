package com.hangout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hangout.dto.AiHangoutRequest;
import com.hangout.dto.AiHangoutResponse;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contract / resilience tests for the LLM integration.
 *
 * <p>A lightweight in-process HTTP server stands in for the Gemini endpoint, so
 * the real {@code RestTemplate} and the real error handling run end-to-end. This
 * covers every failure mode the assignment asks for: overloaded service (503),
 * quota exceeded (429), removed model (404), unparsable output and timeouts.
 * In every case the service must degrade to the rule-based fallback instead of
 * throwing.
 */
@DisplayName("AiService - Gemini integration & resilience")
class AiServiceHttpTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private HttpServer server;
    private int port;
    private AiService aiService;
    private List<Restaurant> catalogue;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        port = server.getAddress().getPort();

        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        catalogue = List.of(
            restaurant("r-it", "Bella Italia", "Italian", Restaurant.PriceRange.MODERATE, 4.7, List.of("Vegetarian")),
            restaurant("r-jp", "Sushi Paradise", "Japanese", Restaurant.PriceRange.EXPENSIVE, 4.5, List.of("Sake"))
        );
        when(restaurantRepository.findAll()).thenReturn(catalogue);

        aiService = new AiService(restaurantRepository);
        ReflectionTestUtils.setField(aiService, "aiEnabled", true);
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "geminiModel", "gemini-test");
        ReflectionTestUtils.setField(aiService, "geminiModels", "gemini-test");
        ReflectionTestUtils.setField(aiService, "geminiUrl", "http://127.0.0.1:" + port + "/v1beta/models");
        ReflectionTestUtils.setField(aiService, "timeoutMs", 2000L);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("happy path: parses the model JSON and marks the result as AI generated")
    void happyPathParsesModelResponse() throws IOException {
        stub(exchange -> send(exchange, 200, geminiEnvelope(aiJson("r-it", "Great Italian Night"))));

        AiHangoutResponse response = aiService.suggestHangout(request("italian dinner", 4));

        assertThat(response.isAiGenerated()).isTrue();
        assertThat(response.getProvider()).isEqualTo("gemini-test");
        assertThat(response.getRestaurantId()).isEqualTo("r-it");
        assertThat(response.getRestaurantName()).isEqualTo("Bella Italia");
        assertThat(response.getTitle()).isEqualTo("Great Italian Night");
        assertThat(response.getNotice()).isNull();
    }

    @Test
    @DisplayName("unknown restaurant id returned by the model is mapped to a real restaurant")
    void unknownRestaurantIdMapsToFirst() throws IOException {
        stub(exchange -> send(exchange, 200, geminiEnvelope(aiJson("does-not-exist", "Fallback pick"))));

        AiHangoutResponse response = aiService.suggestHangout(request("anything", 4));

        assertThat(response.isAiGenerated()).isTrue();
        assertThat(response.getRestaurantId()).isEqualTo("r-it");
    }

    @Test
    @DisplayName("unparsable model output falls back to the rule-based suggestion")
    void malformedResponseFallsBack() throws IOException {
        stub(exchange -> send(exchange, 200, geminiEnvelope("this is definitely not json")));

        AiHangoutResponse response = aiService.suggestHangout(request("italian", 4));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
        assertThat(response.getNotice()).contains("unavailable");
    }

    @Test
    @DisplayName("HTTP 503 (model overloaded) falls back gracefully")
    void serviceUnavailableFallsBack() throws IOException {
        stub(exchange -> send(exchange, 503, "{\"error\":{\"code\":503,\"message\":\"overloaded\"}}"));

        AiHangoutResponse response = aiService.suggestHangout(request("italian", 4));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
        assertThat(response.getNotice()).contains("unavailable");
    }

    @Test
    @DisplayName("HTTP 429 (quota exceeded) falls back gracefully")
    void quotaExceededFallsBack() throws IOException {
        stub(exchange -> send(exchange, 429, "{\"error\":{\"code\":429,\"message\":\"quota\"}}"));

        AiHangoutResponse response = aiService.suggestHangout(request("sushi", 2));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
    }

    @Test
    @DisplayName("HTTP 500 falls back gracefully")
    void serverErrorFallsBack() throws IOException {
        stub(exchange -> send(exchange, 500, "{\"error\":{\"code\":500}}"));

        AiHangoutResponse response = aiService.suggestHangout(request("sushi", 2));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
    }

    @Test
    @DisplayName("a removed model (404) makes the service try the next candidate model")
    void removedModelFallsThroughToNextCandidate() throws IOException {
        ReflectionTestUtils.setField(aiService, "geminiModel", "model-a");
        ReflectionTestUtils.setField(aiService, "geminiModels", "model-a,model-b");

        stub(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.contains("model-a")) {
                send(exchange, 404, "{\"error\":{\"code\":404,\"message\":\"model not found\"}}");
            } else {
                send(exchange, 200, geminiEnvelope(aiJson("r-jp", "Sushi Time")));
            }
        });

        AiHangoutResponse response = aiService.suggestHangout(request("sushi with friends", 3));

        assertThat(response.isAiGenerated()).isTrue();
        assertThat(response.getProvider()).isEqualTo("model-b");
        assertThat(response.getRestaurantId()).isEqualTo("r-jp");
    }

    @Test
    @DisplayName("a read timeout falls back instead of hanging or throwing")
    void timeoutFallsBack() throws IOException {
        ReflectionTestUtils.setField(aiService, "timeoutMs", 300L);

        stub(exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendQuietly(exchange, 200, geminiEnvelope(aiJson("r-it", "Too slow")));
        });

        AiHangoutResponse response = aiService.suggestHangout(request("italian", 4));

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getProvider()).isEqualTo("fallback");
        assertThat(response.getNotice()).contains("unavailable");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void stub(Handler handler) {
        server.createContext("/v1beta/models", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (bytes.length == 0) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private void sendQuietly(HttpExchange exchange, int status, String body) {
        try {
            send(exchange, status, body);
        } catch (IOException ignored) {
            // client already disconnected after its read timeout
        }
    }

    /** Wraps an inner JSON string in the shape Gemini returns. */
    private String geminiEnvelope(String innerJson) throws IOException {
        ObjectNode part = mapper.createObjectNode();
        part.put("text", innerJson);
        ObjectNode content = mapper.createObjectNode();
        content.set("parts", mapper.createArrayNode().add(part));
        ObjectNode candidate = mapper.createObjectNode();
        candidate.set("content", content);
        ObjectNode root = mapper.createObjectNode();
        ArrayNode candidates = mapper.createArrayNode();
        candidates.add(candidate);
        root.set("candidates", candidates);
        return mapper.writeValueAsString(root);
    }

    private String aiJson(String restaurantId, String title) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("restaurantId", restaurantId);
        node.put("title", title);
        node.put("description", "Join us for a great meal");
        node.put("suggestedDateTime", LocalDateTime.now().plusDays(2).withNano(0).toString());
        node.putArray("keywords").add("Dinner").add("Friends");
        node.put("reason", "Because it matches your request");
        node.put("suggestedPartySize", 4);
        return mapper.writeValueAsString(node);
    }

    private AiHangoutRequest request(String prompt, Integer partySize) {
        AiHangoutRequest request = new AiHangoutRequest();
        request.setPrompt(prompt);
        request.setPartySize(partySize);
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

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
