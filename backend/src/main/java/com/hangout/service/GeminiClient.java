package com.hangout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Thin client for the Gemini API (generateContent, JSON output). */
@Component
@Slf4j
public class GeminiClient {

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private static final String FALLBACK_MODEL = "gemini-flash-lite-latest";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.model:gemini-flash-latest}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Sends the prompt and returns the model's JSON answer. If the main model is overloaded
     * or slow, a lighter model is tried so the client still gets an AI answer quickly.
     */
    public JsonNode generateJson(String prompt) throws Exception {
        Exception last = null;
        String[][] attempts = {{model, "15"}, {FALLBACK_MODEL, "12"}};
        for (String[] attempt : attempts) {
            if (attempt[0].equals(model) && attempt != attempts[0]) continue;
            try {
                return call(attempt[0], Integer.parseInt(attempt[1]), prompt);
            } catch (Exception e) {
                last = e;
                log.warn("Gemini model {} failed: {}", attempt[0], e.getMessage());
            }
        }
        throw last != null ? last : new IllegalStateException("Gemini is unavailable");
    }

    private JsonNode call(String modelName, int timeoutSeconds, String prompt) throws Exception {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.4
            )
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(ENDPOINT, modelName)))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            // Log only the status and Google's error message, never the request.
            String message = "";
            try {
                message = mapper.readTree(response.body()).path("error").path("message").asText("");
            } catch (Exception ignored) {
                // keep message empty
            }
            throw new IllegalStateException("Gemini HTTP " + response.statusCode() + (message.isBlank() ? "" : ": " + message));
        }

        // Join the text parts (skipping any "thought" part a reasoning model may add).
        StringBuilder text = new StringBuilder();
        for (JsonNode part : mapper.readTree(response.body()).path("candidates").path(0).path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false) && part.hasNonNull("text")) {
                text.append(part.get("text").asText());
            }
        }
        if (text.toString().isBlank()) {
            throw new IllegalStateException("Gemini returned no content");
        }
        return mapper.readTree(stripFences(text.toString()));
    }

    private static String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return s;
    }
}
