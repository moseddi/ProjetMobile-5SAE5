package com.hangout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.dto.loyalty.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Mon Coach Fidelite" — the loyalty program's conversational assistant.
 *
 * <p>The backend builds a small, verified context (real balance, real game
 * catalog with today's eligibility, real rewards with real costs) and sends
 * only that to the LLM. The model never invents balances or IDs: every
 * recommendation it returns is cross-checked against the actual catalog
 * before being sent to the client, and anything it gets wrong is dropped
 * rather than shown to the user.
 *
 * <p>When no API key is configured, or the provider call fails, a
 * deterministic rule-based responder (built from the same verified context)
 * answers instead, clearly flagged as non-AI via {@code aiGenerated=false}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoyaltyAssistantService {

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.models:gemini-3.5-flash,gemini-3-flash-preview,gemini-3.1-flash-lite,gemini-flash-lite-latest}")
    private String geminiModels;

    @Value("${ai.gemini.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiUrl;

    @Value("${ai.timeout-ms:15000}")
    private long timeoutMs;

    public AssistantChatResponse chat(String userId, String message) {
        LoyaltyDashboardResponse dashboard = loyaltyService.getDashboard(userId);
        List<RewardDto> rewards = loyaltyService.getRewards(userId);

        boolean keyConfigured = geminiApiKey != null && !geminiApiKey.isBlank();
        if (aiEnabled && keyConfigured) {
            try {
                AssistantChatResponse response = callGemini(message, dashboard, rewards);
                if (response != null) {
                    validateAndSanitize(response, dashboard, rewards);
                    response.setAiGenerated(true);
                    return response;
                }
                log.warn("Loyalty assistant: AI provider returned an unusable response, using fallback");
            } catch (Exception ex) {
                log.warn("Loyalty assistant: AI provider call failed ({}), using fallback", ex.getMessage());
            }
        }

        AssistantChatResponse fallback = ruleBasedResponse(message, dashboard, rewards);
        fallback.setAiGenerated(false);
        fallback.setNotice(keyConfigured
            ? "L'assistant IA est momentanement indisponible. Voici une reponse basee sur des regles."
            : "Aucune cle API IA n'est configuree. Voici une reponse basee sur des regles.");
        return fallback;
    }

    // ------------------------------------------------- restaurant recommendations

    /**
     * Writes a short, personalized French explanation for each already-ranked
     * candidate (restaurant + reward). The candidates themselves are decided entirely
     * by {@link com.hangout.service.RestaurantRecommendationService} before this is
     * called: the model only explains, it never picks, prices or invents a candidate.
     * Every (restaurantId, rewardId) pair in the response is checked against the real
     * candidate list; anything that doesn't match is dropped and replaced by a
     * deterministic sentence built from the verified data instead.
     */
    public RecommendationExplanations explainRestaurantRecommendations(int balance, String goal, String userMessage,
                                                                        List<RecommendationCandidate> candidates) {
        RecommendationExplanations result = new RecommendationExplanations();
        boolean keyConfigured = geminiApiKey != null && !geminiApiKey.isBlank();

        if (!candidates.isEmpty() && aiEnabled && keyConfigured) {
            try {
                RecommendationExplanations aiResult = callGeminiForRecommendations(balance, goal, userMessage, candidates);
                if (aiResult != null) {
                    sanitizeRecommendationExplanations(aiResult, candidates);
                    aiResult.setAiGenerated(true);
                    return aiResult;
                }
                log.warn("Loyalty recommendations: AI provider returned an unusable response, using fallback");
            } catch (Exception ex) {
                log.warn("Loyalty recommendations: AI provider call failed ({}), using fallback", ex.getMessage());
            }
        }

        fillDeterministicReasons(result, candidates);
        result.setOverallMessage(deterministicOverallMessage(balance, candidates));
        result.setAiGenerated(false);
        result.setNotice(candidates.isEmpty() ? null : keyConfigured
            ? "L'assistant IA est momentanement indisponible. Explications basees sur des regles."
            : "Aucune cle API IA n'est configuree. Explications basees sur des regles.");
        return result;
    }

    private RecommendationExplanations callGeminiForRecommendations(int balance, String goal, String userMessage,
                                                                      List<RecommendationCandidate> candidates) throws Exception {
        List<String> models = new ArrayList<>();
        if (geminiModels != null) {
            for (String m : geminiModels.split(",")) {
                if (!m.isBlank() && !models.contains(m.trim())) {
                    models.add(m.trim());
                }
            }
        }
        if (models.isEmpty()) {
            models.add("gemini-3.5-flash");
        }

        Exception lastError = null;
        for (String model : models) {
            try {
                RecommendationExplanations result = callGeminiRecommendationModel(balance, goal, userMessage, candidates, model);
                if (result != null) {
                    return result;
                }
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Loyalty recommendations: Gemini model '{}' failed ({}), trying next candidate", model, ex.getMessage());
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    private RecommendationExplanations callGeminiRecommendationModel(int balance, String goal, String userMessage,
                                                                      List<RecommendationCandidate> candidates, String model) throws Exception {
        String systemPrompt = "Tu es 'Mon Coach Fidelite'. Tu rediges de courtes explications personnalisees en francais pour "
            + "expliquer pourquoi chaque restaurant propose est une bonne option pour depenser les points de fidelite du client. "
            + "Tu dois IMPERATIVEMENT utiliser UNIQUEMENT les candidats fournis ci-dessous : n'invente, ne renomme et ne reordonne "
            + "jamais un restaurant, une recompense, un identifiant ou un cout en points. Une phrase par candidat, concise et chaleureuse. "
            + "Reponds UNIQUEMENT par un objet JSON valide avec les cles: "
            + "overallMessage (string, 1 a 2 phrases d'introduction generale), "
            + "items (tableau d'objets avec restaurantId, rewardId -- recopies exactement tels que fournis -- et reason).";

        StringBuilder context = new StringBuilder();
        context.append("Solde actuel du client: ").append(balance).append(" points.\n");
        if (goal != null && !goal.isBlank()) {
            context.append("Objectif du client: ").append(goal).append(".\n");
        }
        context.append("\nCandidats verifies (ne pas en ajouter, ne pas en retirer, ne pas modifier) :\n");
        for (RecommendationCandidate c : candidates) {
            context.append("- restaurantId=").append(c.restaurantId())
                .append(" | rewardId=").append(c.rewardId())
                .append(" | restaurant=").append(c.restaurantName())
                .append(" | cuisine=").append(c.cuisine() != null ? c.cuisine() : "inconnue")
                .append(" | gamme=").append(c.priceRange() != null ? c.priceRange() : "inconnue")
                .append(" | note=").append(c.rating() != null ? c.rating() : "non notee")
                .append(" | recompense=").append(c.rewardName())
                .append(" | cout=").append(c.pointsCost()).append(" points")
                .append(" | accessible=").append(c.affordable() ? "oui" : "non, manque " + c.pointsMissing() + " points")
                .append(" | preference_cuisine=").append(c.matchesPreferredCuisine() ? "correspond aux habitudes du client" : "non")
                .append(c.expiryDate() != null ? " | expire_le=" + c.expiryDate() : "")
                .append("\n");
        }
        if (userMessage != null && !userMessage.isBlank()) {
            context.append("\nMessage du client: ").append(userMessage);
        }

        Map<String, Object> body = Map.of(
            "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
            "contents", List.of(Map.of("parts", List.of(Map.of("text", context.toString())))),
            "generationConfig", Map.of("temperature", 0.5, "responseMimeType", "application/json")
        );

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
        return parseRecommendationJson(text);
    }

    private RecommendationExplanations parseRecommendationJson(String text) {
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
            RecommendationExplanations result = new RecommendationExplanations();
            result.setOverallMessage(node.path("overallMessage").asText(null));
            JsonNode items = node.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    String restaurantId = item.path("restaurantId").asText(null);
                    String rewardId = item.path("rewardId").asText(null);
                    String reason = item.path("reason").asText(null);
                    if (restaurantId != null && rewardId != null && reason != null) {
                        result.getReasonsByKey().put(restaurantId + "|" + rewardId, reason);
                    }
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Loyalty recommendations: failed to parse AI response: {}", ex.getMessage());
            return null;
        }
    }

    /** Drops any explanation whose (restaurantId, rewardId) pair is not one of the real candidates. */
    private void sanitizeRecommendationExplanations(RecommendationExplanations result, List<RecommendationCandidate> candidates) {
        var validKeys = candidates.stream().map(RecommendationCandidate::key).collect(Collectors.toSet());
        result.getReasonsByKey().keySet().retainAll(validKeys);
        for (RecommendationCandidate c : candidates) {
            if (!result.getReasonsByKey().containsKey(c.key())) {
                result.getReasonsByKey().put(c.key(), deterministicReason(c));
            }
        }
        if (result.getOverallMessage() == null || result.getOverallMessage().isBlank()) {
            result.setOverallMessage(deterministicOverallMessage(candidates.size(), candidates));
        }
    }

    private void fillDeterministicReasons(RecommendationExplanations result, List<RecommendationCandidate> candidates) {
        for (RecommendationCandidate c : candidates) {
            result.getReasonsByKey().put(c.key(), deterministicReason(c));
        }
    }

    private String deterministicReason(RecommendationCandidate c) {
        StringBuilder sb = new StringBuilder();
        if (c.affordable()) {
            sb.append("Tu peux echanger \"").append(c.rewardName()).append("\" des maintenant avec ton solde actuel.");
        } else {
            sb.append("Il te manque ").append(c.pointsMissing()).append(" points pour obtenir \"").append(c.rewardName()).append("\".");
        }
        if (c.matchesPreferredCuisine()) {
            sb.append(" Cette cuisine correspond a tes habitudes recentes.");
        }
        if (c.rating() != null && c.rating() >= 4.5) {
            sb.append(" Tres bien note (").append(c.rating()).append("/5).");
        }
        return sb.toString();
    }

    private String deterministicOverallMessage(int balance, List<RecommendationCandidate> candidates) {
        if (candidates.isEmpty()) {
            return "Aucun restaurant partenaire ne propose de recompense correspondant a ton profil pour le moment.";
        }
        long affordableCount = candidates.stream().filter(RecommendationCandidate::affordable).count();
        return affordableCount > 0
            ? "Avec " + balance + " points, voici les meilleures options pour en profiter des maintenant."
            : "Avec " + balance + " points, voici les recompenses les plus proches de ton solde actuel.";
    }

    // --------------------------------------------------------------- gemini

    private AssistantChatResponse callGemini(String message, LoyaltyDashboardResponse dashboard, List<RewardDto> rewards) throws Exception {
        List<String> candidates = new ArrayList<>();
        if (geminiModels != null) {
            for (String m : geminiModels.split(",")) {
                if (!m.isBlank() && !candidates.contains(m.trim())) {
                    candidates.add(m.trim());
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates.add("gemini-3.5-flash");
        }

        Exception lastError = null;
        for (String model : candidates) {
            try {
                AssistantChatResponse response = callGeminiModel(message, dashboard, rewards, model);
                if (response != null) {
                    return response;
                }
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Loyalty assistant: Gemini model '{}' failed ({}), trying next candidate", model, ex.getMessage());
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        return null;
    }

    private AssistantChatResponse callGeminiModel(String message, LoyaltyDashboardResponse dashboard, List<RewardDto> rewards, String model) throws Exception {
        String systemPrompt = "Tu es 'Mon Coach Fidelite', l'assistant du programme de fidelite d'une application de restaurants. "
            + "Tu reponds UNIQUEMENT en francais, de maniere concise, chaleureuse et utile. "
            + "Tu dois IMPERATIVEMENT t'appuyer seulement sur les donnees verifiees fournies ci-dessous (solde, jeux, recompenses, defis) : "
            + "n'invente jamais un solde, un jeu ou une recompense qui n'y figure pas. "
            + "Si la question est ambigue, pose une question de clarification courte plutot que d'inventer une reponse. "
            + "Reponds UNIQUEMENT par un objet JSON valide, sans texte autour, avec les cles: "
            + "intent (une valeur parmi RECOMMEND_GAMES, RECOMMEND_REWARDS, EXPLAIN_POINTS_BALANCE, EXPLAIN_REDEMPTION, EARN_MORE_POINTS, TRACK_LOYALTY_PROGRESS, EXPLAIN_GAME_RULES, COMPARE_REWARDS, UPDATE_PREFERENCES, GENERAL_LOYALTY_QUESTION), "
            + "message (string, ta reponse conversationnelle en francais), "
            + "recommendations (tableau d'objets avec type: GAME ou REWARD, id qui doit imperativement etre l'un des identifiants fournis, title, reason, action parmi VIEW_GAME, START_GAME, VIEW_REWARD, REDEEM_REWARD), "
            + "followUpQuestion (string ou null).";

        String context = buildContext(dashboard, rewards);

        Map<String, Object> body = Map.of(
            "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
            "contents", List.of(Map.of("parts", List.of(Map.of("text", context + "\n\nQuestion de l'utilisateur: " + message)))),
            "generationConfig", Map.of("temperature", 0.6, "responseMimeType", "application/json")
        );

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
        return parseAiJson(text);
    }

    private String buildContext(LoyaltyDashboardResponse dashboard, List<RewardDto> rewards) {
        StringBuilder sb = new StringBuilder();
        sb.append("Solde actuel: ").append(dashboard.getPoints()).append(" points. ");
        sb.append("Niveau: ").append(dashboard.getTier());
        if (dashboard.getNextTier() != null) {
            sb.append(" (").append(dashboard.getPointsToNextTier()).append(" points pour atteindre ").append(dashboard.getNextTier()).append(")");
        }
        sb.append(".\n\nJeux disponibles aujourd'hui:\n");
        for (GameCatalogEntry g : dashboard.getGames()) {
            sb.append("- id=").append(g.getId())
                .append(" | nom=").append(g.getName())
                .append(" | disponible=").append(g.isAvailableToday() ? "oui" : "non (limite quotidienne atteinte)")
                .append(" | duree=~").append(g.getEstimatedDurationMinutes()).append("min")
                .append(" | points=").append(g.getPointsReward())
                .append(" | regles=").append(g.getRules())
                .append("\n");
        }
        sb.append("\nDefis du jour:\n");
        for (ChallengeDto c : dashboard.getTodaysChallenges()) {
            sb.append("- ").append(c.getTitle()).append(": ").append(c.getProgress()).append("/").append(c.getTarget())
                .append(c.isCompleted() ? " (complete)" : "").append(" (+").append(c.getPointsReward()).append(" points)\n");
        }
        sb.append("\nRecompenses disponibles:\n");
        for (RewardDto r : rewards) {
            sb.append("- id=").append(r.getId())
                .append(" | nom=").append(r.getName())
                .append(" | cout=").append(r.getPointsCost()).append(" points")
                .append(" | accessible=").append(r.isAffordable() ? "oui" : "non, manque " + r.getPointsMissing() + " points")
                .append(" | categorie=").append(r.getCategory())
                .append("\n");
        }
        return sb.toString();
    }

    private AssistantChatResponse parseAiJson(String text) {
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
            return objectMapper.readValue(cleaned, AssistantChatResponse.class);
        } catch (Exception ex) {
            log.warn("Loyalty assistant: failed to parse AI response: {}", ex.getMessage());
            return null;
        }
    }

    /** Drops any recommendation whose id does not match a real game/reward — the
     *  model must never be able to point the user at something that does not exist. */
    private void validateAndSanitize(AssistantChatResponse response, LoyaltyDashboardResponse dashboard, List<RewardDto> rewards) {
        if (response.getMessage() == null || response.getMessage().isBlank()) {
            response.setMessage("Voici ce que je peux te proposer.");
        }
        if (response.getRecommendations() == null) {
            response.setRecommendations(new ArrayList<>());
            return;
        }
        var validGameIds = dashboard.getGames().stream().map(GameCatalogEntry::getId).collect(Collectors.toSet());
        var validRewardIds = rewards.stream().map(RewardDto::getId).collect(Collectors.toSet());

        List<AssistantChatResponse.Recommendation> sanitized = response.getRecommendations().stream()
            .filter(r -> r.getId() != null && r.getType() != null)
            .filter(r -> ("GAME".equals(r.getType()) && validGameIds.contains(r.getId()))
                || ("REWARD".equals(r.getType()) && validRewardIds.contains(r.getId())))
            .limit(5)
            .collect(Collectors.toList());
        response.setRecommendations(sanitized);
    }

    // ------------------------------------------------------------- fallback

    private AssistantChatResponse ruleBasedResponse(String message, LoyaltyDashboardResponse dashboard, List<RewardDto> rewards) {
        String lower = message == null ? "" : message.toLowerCase();
        AssistantChatResponse response = new AssistantChatResponse();
        List<AssistantChatResponse.Recommendation> recs = new ArrayList<>();

        boolean asksGames = containsAny(lower, "jeu", "jouer", "jeux", "game");
        boolean asksRewards = containsAny(lower, "recompense", "récompense", "offrir", "obtenir", "reward", "echanger", "échanger");
        boolean asksBalance = containsAny(lower, "solde", "combien de points", "mes points", "balance");
        boolean asksEarn = containsAny(lower, "gagner", "plus de points", "comment faire");

        if (asksGames || (!asksRewards && !asksBalance && !asksEarn)) {
            response.setIntent("RECOMMEND_GAMES");
            List<GameCatalogEntry> available = dashboard.getGames().stream().filter(GameCatalogEntry::isAvailableToday).collect(Collectors.toList());
            if (available.isEmpty()) {
                response.setMessage("Tu as deja joue a tous les jeux disponibles aujourd'hui. Reviens demain pour de nouvelles parties !");
            } else {
                String names = available.stream().map(GameCatalogEntry::getName).collect(Collectors.joining(", "));
                response.setMessage("Voici ce que tu peux jouer aujourd'hui : " + names + ".");
                available.forEach(g -> recs.add(rec("GAME", g.getId(), g.getName(), g.getRules(), "VIEW_GAME")));
            }
        } else if (asksRewards) {
            response.setIntent("RECOMMEND_REWARDS");
            List<RewardDto> affordable = rewards.stream().filter(RewardDto::isAffordable).limit(3).collect(Collectors.toList());
            if (affordable.isEmpty()) {
                RewardDto cheapest = rewards.stream().min((a, b) -> a.getPointsCost() - b.getPointsCost()).orElse(null);
                if (cheapest != null) {
                    response.setMessage("Avec " + dashboard.getPoints() + " points, tu ne peux pas encore echanger de recompense. Il te manque "
                        + cheapest.getPointsMissing() + " points pour obtenir \"" + cheapest.getName() + "\".");
                    recs.add(rec("REWARD", cheapest.getId(), cheapest.getName(), "La recompense la plus proche de ton solde actuel", "VIEW_REWARD"));
                } else {
                    response.setMessage("Aucune recompense n'est disponible pour le moment.");
                }
            } else {
                response.setMessage("Avec " + dashboard.getPoints() + " points, tu peux obtenir : "
                    + affordable.stream().map(RewardDto::getName).collect(Collectors.joining(", ")) + ".");
                affordable.forEach(r -> recs.add(rec("REWARD", r.getId(), r.getName(), r.getPointsCost() + " points", "VIEW_REWARD")));
            }
        } else if (asksEarn) {
            response.setIntent("EARN_MORE_POINTS");
            List<GameCatalogEntry> available = dashboard.getGames().stream().filter(GameCatalogEntry::isAvailableToday).collect(Collectors.toList());
            response.setMessage(available.isEmpty()
                ? "Tu as deja utilise toutes tes tentatives du jour. Reviens demain pour gagner plus de points !"
                : "Pour gagner des points des maintenant, commence par : " + available.get(0).getName() + ".");
            available.forEach(g -> recs.add(rec("GAME", g.getId(), g.getName(), g.getRules(), "START_GAME")));
        } else {
            response.setIntent("EXPLAIN_POINTS_BALANCE");
            response.setMessage("Tu as actuellement " + dashboard.getPoints() + " points (niveau " + dashboard.getTier() + ").");
        }

        response.setRecommendations(recs);
        response.setFollowUpQuestion(null);
        return response;
    }

    private AssistantChatResponse.Recommendation rec(String type, String id, String title, String reason, String action) {
        AssistantChatResponse.Recommendation r = new AssistantChatResponse.Recommendation();
        r.setType(type);
        r.setId(id);
        r.setTitle(title);
        r.setReason(reason);
        r.setAction(action);
        return r;
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) return true;
        }
        return false;
    }
}
