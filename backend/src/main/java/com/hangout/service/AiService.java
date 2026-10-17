package com.hangout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.dto.AiHangoutRequest;
import com.hangout.dto.AiHangoutResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI module of the HangOut application.
 *
 * <p>It calls an LLM (Google Gemini by default) to suggest a hangout from a
 * natural-language description: it picks the best matching restaurant and writes
 * a catchy title, an invitation message, a suggested date/time and keywords.
 *
 * <p>Only restaurant catalogue data (never personal data) is sent to the model.
 * Every failure mode required by the assignment is handled gracefully:
 * missing API key, timeout, quota/HTTP error and unparsable output all fall back
 * to a deterministic rule-based recommendation so the app keeps working.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    private final RestaurantRepository restaurantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.provider:gemini}")
    private String provider;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model:gemini-3.5-flash}")
    private String geminiModel;

    /**
     * Ordered list of candidate models. The service tries them one by one so a
     * single overloaded/deprecated model does not disable the whole AI module.
     */
    @Value("${ai.gemini.models:gemini-3.5-flash,gemini-3-flash-preview,gemini-3.1-flash-lite,gemini-flash-lite-latest}")
    private String geminiModels;

    @Value("${ai.gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiUrl;

    @Value("${ai.timeout-ms:15000}")
    private long timeoutMs;

    public AiHangoutResponse suggestHangout(AiHangoutRequest request) {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            throw new BadRequestException("No restaurants available to recommend");
        }

        boolean keyConfigured = geminiApiKey != null && !geminiApiKey.isBlank();

        if (aiEnabled && keyConfigured) {
            try {
                AiHangoutResponse response = callGemini(request, restaurants);
                if (response != null) {
                    return response;
                }
                log.warn("AI provider returned an unusable response, using fallback");
            } catch (Exception ex) {
                log.warn("AI provider call failed ({}), using fallback", ex.getMessage());
            }
        }

        String notice = keyConfigured
            ? "The AI provider is unavailable right now (timeout, quota or error). A rule-based suggestion is shown instead."
            : "No AI API key is configured. A rule-based suggestion is shown instead.";
        return buildFallback(request, restaurants, notice);
    }

    private AiHangoutResponse callGemini(AiHangoutRequest request, List<Restaurant> restaurants) throws Exception {
        List<String> candidates = new ArrayList<>();
        if (geminiModels != null) {
            for (String m : geminiModels.split(",")) {
                if (!m.isBlank() && !candidates.contains(m.trim())) {
                    candidates.add(m.trim());
                }
            }
        }
        if (geminiModel != null && !geminiModel.isBlank() && !candidates.contains(geminiModel.trim())) {
            candidates.add(0, geminiModel.trim());
        }
        if (candidates.isEmpty()) {
            candidates.add("gemini-3.5-flash");
        }

        Exception lastError = null;
        for (String model : candidates) {
            try {
                AiHangoutResponse response = callGeminiModel(request, restaurants, model);
                if (response != null) {
                    return response;
                }
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Gemini model '{}' failed ({}), trying next candidate", model, ex.getMessage());
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    private AiHangoutResponse callGeminiModel(AiHangoutRequest request, List<Restaurant> restaurants, String model) throws Exception {
        String systemPrompt = "Tu es un assistant expert en organisation de sorties restaurants pour l'application HangOut. "
            + "A partir de la description en langage naturel d'une sortie de groupe et de la liste des restaurants disponibles, "
            + "choisis le restaurant le plus adapte et prepare une invitation. "
            + "Reponds UNIQUEMENT par un objet JSON valide, sans texte autour, avec les cles: "
            + "restaurantId (string, doit imperativement etre l'un des ids fournis), "
            + "title (string court et accrocheur, max 80 caracteres), "
            + "description (string, message d'invitation de 2 a 3 phrases), "
            + "suggestedDateTime (string au format ISO-8601 'yyyy-MM-ddTHH:mm', dans le futur), "
            + "keywords (tableau JSON de 3 a 5 mots-cles), "
            + "reason (string, 1 a 2 phrases expliquant le choix du restaurant), "
            + "suggestedPartySize (entier).";

        String restaurantList = restaurants.stream()
            .map(r -> String.format("- id=%s | nom=%s | cuisine=%s | gamme=%s | note=%s | caracteristiques=%s | description=%s",
                r.getId(), r.getName(), r.getCuisine(),
                r.getPriceRange() != null ? r.getPriceRange().name() : "N/A",
                r.getRating() != null ? r.getRating() : "N/A",
                r.getFeatures() != null ? String.join(", ", r.getFeatures()) : "",
                r.getDescription() != null ? r.getDescription() : ""))
            .collect(Collectors.joining("\n"));

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Date et heure actuelles: ").append(LocalDateTime.now().withNano(0)).append("\n");
        userPrompt.append("Description de la sortie: ").append(request.getPrompt());
        if (request.getPartySize() != null) {
            userPrompt.append("\nNombre de personnes: ").append(request.getPartySize());
        }
        if (request.getBudget() != null && !request.getBudget().isBlank()) {
            userPrompt.append("\nBudget indicatif: ").append(request.getBudget());
        }
        userPrompt.append("\n\nRestaurants disponibles:\n").append(restaurantList);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt.toString())))));
        body.put("generationConfig", Map.of(
            "temperature", 0.7,
            "responseMimeType", "application/json"));

        String url = geminiUrl + "/" + model + ":generateContent?key=" + geminiApiKey;

        RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(timeoutMs))
            .setReadTimeout(Duration.ofMillis(timeoutMs))
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        String text = parts.get(0).path("text").asText("");
        return parseAiJson(text, request, restaurants, model);
    }

    private AiHangoutResponse parseAiJson(String text, AiHangoutRequest request, List<Restaurant> restaurants, String model) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*", "").replaceAll("```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            String restaurantId = node.path("restaurantId").asText("");
            Restaurant matched = restaurants.stream()
                .filter(r -> r.getId().equals(restaurantId))
                .findFirst()
                .orElse(restaurants.get(0));

            AiHangoutResponse resp = new AiHangoutResponse();
            resp.setRestaurantId(matched.getId());
            resp.setRestaurantName(matched.getName());
            resp.setTitle(node.path("title").asText(defaultTitle(request)));
            resp.setDescription(node.path("description").asText(defaultDescription(request, matched)));
            resp.setSuggestedDateTime(sanitizeDateTime(node.path("suggestedDateTime").asText(defaultDateTime())));

            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = node.path("keywords");
            if (keywordsNode.isArray()) {
                for (JsonNode k : keywordsNode) {
                    keywords.add(k.asText());
                }
            }
            if (keywords.isEmpty()) {
                keywords = defaultKeywords(request, matched);
            }
            resp.setKeywords(keywords);
            resp.setReason(node.path("reason").asText(""));
            resp.setSuggestedPartySize(node.path("suggestedPartySize")
                .asInt(request.getPartySize() != null ? request.getPartySize() : 4));
            resp.setAiGenerated(true);
            resp.setProvider(model != null ? model : "gemini");
            return resp;
        } catch (Exception ex) {
            log.warn("Failed to parse AI response: {}", ex.getMessage());
            return null;
        }
    }

    private AiHangoutResponse buildFallback(AiHangoutRequest request, List<Restaurant> restaurants, String notice) {
        Restaurant best = pickRestaurant(request, restaurants);

        AiHangoutResponse resp = new AiHangoutResponse();
        resp.setRestaurantId(best.getId());
        resp.setRestaurantName(best.getName());
        resp.setTitle(defaultTitle(request));
        resp.setDescription(defaultDescription(request, best));
        resp.setSuggestedDateTime(defaultDateTime());
        resp.setKeywords(defaultKeywords(request, best));
        resp.setReason("Recommended automatically: " + (best.getCuisine() != null ? best.getCuisine() + " cuisine" : "great food")
            + (best.getRating() != null ? " (rated " + best.getRating() + "/5)" : "")
            + (best.getPriceRange() != null ? ", " + best.getPriceRange().name().toLowerCase() + " price range" : "") + ".");
        resp.setSuggestedPartySize(request.getPartySize() != null ? request.getPartySize() : 4);
        resp.setAiGenerated(false);
        resp.setProvider("fallback");
        resp.setNotice(notice);
        return resp;
    }

    private Restaurant pickRestaurant(AiHangoutRequest request, List<Restaurant> restaurants) {
        String prompt = request.getPrompt() != null ? request.getPrompt().toLowerCase() : "";
        Restaurant best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Restaurant r : restaurants) {
            int score = 0;
            score += cuisineScore(prompt, r.getCuisine());
            if (r.getName() != null && prompt.contains(r.getName().toLowerCase())) score += 3;
            score += budgetScore(prompt, r.getPriceRange());
            if (r.getFeatures() != null) {
                for (String f : r.getFeatures()) {
                    if (f != null && prompt.contains(f.toLowerCase())) score += 1;
                }
            }
            if (r.getRating() != null && r.getRating() >= 4.5) score += 1;

            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }

        if (best == null) {
            best = restaurants.stream()
                .max(Comparator.comparingDouble(r -> r.getRating() != null ? r.getRating() : 0.0))
                .orElse(restaurants.get(0));
        }
        return best;
    }

    private int cuisineScore(String prompt, String cuisine) {
        if (cuisine == null) {
            return 0;
        }
        String c = cuisine.toLowerCase();
        if (prompt.contains(c)) {
            return 3;
        }
        switch (c) {
            case "italian":
                if (containsAny(prompt, "ital", "pizza", "pasta", "pizzeria")) return 3;
                break;
            case "japanese":
                if (containsAny(prompt, "japon", "sushi", "ramen")) return 3;
                break;
            case "french":
                if (containsAny(prompt, "franc", "bistrot", "bistro")) return 3;
                break;
            case "indian":
                if (containsAny(prompt, "indien", "indienne", "curry")) return 3;
                break;
            default:
                break;
        }
        return 0;
    }

    private int budgetScore(String prompt, Restaurant.PriceRange priceRange) {
        if (priceRange == null) {
            return 0;
        }
        String name = priceRange.name().toLowerCase();
        if (prompt.contains(name)) {
            return 2;
        }
        switch (priceRange) {
            case BUDGET:
                if (containsAny(prompt, "budget", "pas cher", "cheap", "abordable", "economique")) return 2;
                break;
            case MODERATE:
                if (containsAny(prompt, "moyen", "modere", "modéré", "medium", "normal")) return 2;
                break;
            case EXPENSIVE:
                if (containsAny(prompt, "cher", "expensive", "haut de gamme", "chic")) return 2;
                break;
            case LUXURY:
                if (containsAny(prompt, "luxe", "luxury", "prestige", "etoile", "étoilé")) return 2;
                break;
            default:
                break;
        }
        return 0;
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private String defaultTitle(AiHangoutRequest request) {
        String p = request.getPrompt() != null ? request.getPrompt().toLowerCase() : "";
        if (p.contains("anniversaire") || p.contains("birthday")) return "Birthday Celebration Dinner";
        if (p.contains("brunch")) return "Weekend Brunch Get-Together";
        if (p.contains("business") || p.contains("travail") || p.contains("reunion") || p.contains("réunion")) return "Business Lunch Meetup";
        if (p.contains("famille") || p.contains("family")) return "Family Dinner Out";
        return "Let's Hang Out & Dine Together";
    }

    private String defaultDateTime() {
        return LocalDateTime.now().plusDays(3)
            .withHour(19).withMinute(30).withSecond(0).withNano(0)
            .toString();
    }

    /**
     * The LLM has no sense of "now", so it may suggest a past date. Any date that
     * is not comfortably in the future is replaced by the default (now + 3 days).
     */
    private String sanitizeDateTime(String value) {
        if (value != null && !value.isBlank()) {
            try {
                LocalDateTime dt = LocalDateTime.parse(value.trim().replace(" ", "T"));
                if (dt.isAfter(LocalDateTime.now().plusHours(1))) {
                    return dt.withSecond(0).withNano(0).toString();
                }
            } catch (Exception ex) {
                log.debug("Could not parse AI date '{}': {}", value, ex.getMessage());
            }
        }
        return defaultDateTime();
    }

    private String defaultDescription(AiHangoutRequest request, Restaurant r) {
        return "Let's get together for a great meal at " + r.getName() + "! We'll enjoy "
            + (r.getCuisine() != null ? r.getCuisine().toLowerCase() : "delicious") + " food and good company. "
            + "Join us and bring your appetite!";
    }

    private List<String> defaultKeywords(AiHangoutRequest request, Restaurant r) {
        List<String> keywords = new ArrayList<>();
        if (r.getCuisine() != null) keywords.add(r.getCuisine());
        keywords.add("Dinner");
        if (r.getFeatures() != null) {
            r.getFeatures().stream().limit(2).forEach(keywords::add);
        }
        if (request.getPartySize() != null) keywords.add(request.getPartySize() + " people");
        return keywords;
    }
}
