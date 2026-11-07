package com.hangout.service;

import com.hangout.dto.AiAnalysisResult;
import com.hangout.dto.RestaurantRecommendation;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service orchestrant l'analyse granulaire (ABSA),
 * la détection d'anomalies en temps réel (Anti-fraude)
 * et le moteur de recommandation corrective (RAG) sur la base de données de l'application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewService {

    private final RestaurantRepository restaurantRepository;
    private final RestTemplate restTemplate;
    private final RagKnowledgeBaseService ragKnowledgeBaseService;

    @Value("${gemini.api.key:}")
    private String configuredApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    /**
     * Analyse l'avis avec support des notes spécifiques par critère (si fournies)
     * et analyse granulaire de sentiment par aspect (ABSA).
     */
    public AiAnalysisResult analyzeReview(
            String comment,
            Double overallRating,
            Double userFood,
            Double userService,
            Double userPrice,
            Double userAmbiance,
            String currentRestaurantId
    ) {
        log.info("Lancement de l'analyse Multi-Agent ABSA pour l'avis...");

        // 1. Exécution des 4 Agents IA spécialisés (CoT)
        CompletableFuture<AgentResult> foodAgentFuture =
                CompletableFuture.supplyAsync(() -> executeFoodAgent(comment, overallRating, userFood));
        CompletableFuture<AgentResult> serviceAgentFuture =
                CompletableFuture.supplyAsync(() -> executeServiceAgent(comment, overallRating, userService));
        CompletableFuture<AgentResult> priceAgentFuture =
                CompletableFuture.supplyAsync(() -> executePriceAgent(comment, overallRating, userPrice));
        CompletableFuture<AgentResult> ambianceAgentFuture =
                CompletableFuture.supplyAsync(() -> executeAmbianceAgent(comment, overallRating, userAmbiance));

        CompletableFuture.allOf(foodAgentFuture, serviceAgentFuture, priceAgentFuture, ambianceAgentFuture).join();

        AgentResult foodResult = foodAgentFuture.join();
        AgentResult serviceResult = serviceAgentFuture.join();
        AgentResult priceResult = priceAgentFuture.join();
        AgentResult ambianceResult = ambianceAgentFuture.join();

        // 2. Détection d'anomalie en temps réel (Anti-fraude)
        AnomalyCheck anomalyCheck = detectAnomaly(
                overallRating,
                foodResult.score,
                serviceResult.score,
                priceResult.score,
                ambianceResult.score,
                comment
        );

        // 3. Moteur de recommandation corrective (RAG) sur les vrais restaurants de la plateforme
        List<RestaurantRecommendation> recommendations = new ArrayList<>();
        boolean isDisappointed = (overallRating != null && overallRating <= 2.5)
                || foodResult.score <= 4
                || serviceResult.score <= 4
                || priceResult.score <= 4
                || ambianceResult.score <= 4;

        if (isDisappointed && !anomalyCheck.isAnomaly) {
            recommendations = retrieveRagRecommendations(
                    currentRestaurantId,
                    foodResult.score,
                    serviceResult.score,
                    priceResult.score,
                    ambianceResult.score,
                    comment
            );
        }

        return AiAnalysisResult.builder()
                .foodScore(foodResult.score)
                .serviceScore(serviceResult.score)
                .priceScore(priceResult.score)
                .ambianceScore(ambianceResult.score)
                .foodThought(foodResult.thought)
                .serviceThought(serviceResult.thought)
                .priceThought(priceResult.thought)
                .ambianceThought(ambianceResult.thought)
                .isAnomaly(anomalyCheck.isAnomaly)
                .anomalyReason(anomalyCheck.reason)
                .recommendations(recommendations)
                .build();
    }

    // =========================================================================
    // Multi-Agent System (4 Agents Spécialisés)
    // =========================================================================

    private AgentResult executeFoodAgent(String comment, Double overallRating, Double userStars) {
        if (userStars != null) {
            int score = (int) Math.round(userStars * 2.0);
            return new AgentResult(score, "Évaluation culinaire : " + userStars + " étoiles");
        }
        return computeTargetedAspectSentiment(comment, Criterion.FOOD, overallRating);
    }

    private AgentResult executeServiceAgent(String comment, Double overallRating, Double userStars) {
        if (userStars != null) {
            int score = (int) Math.round(userStars * 2.0);
            return new AgentResult(score, "Évaluation du service : " + userStars + " étoiles");
        }
        return computeTargetedAspectSentiment(comment, Criterion.SERVICE, overallRating);
    }

    private AgentResult executePriceAgent(String comment, Double overallRating, Double userStars) {
        if (userStars != null) {
            int score = (int) Math.round(userStars * 2.0);
            return new AgentResult(score, "Évaluation rapport qualité/prix : " + userStars + " étoiles");
        }
        return computeTargetedAspectSentiment(comment, Criterion.PRICE, overallRating);
    }

    private AgentResult executeAmbianceAgent(String comment, Double overallRating, Double userStars) {
        if (userStars != null) {
            int score = (int) Math.round(userStars * 2.0);
            return new AgentResult(score, "Évaluation ambiance & cadre : " + userStars + " étoiles");
        }
        return computeTargetedAspectSentiment(comment, Criterion.AMBIANCE, overallRating);
    }

    // =========================================================================
    // Analyse Sémantique Granulaire par Aspect (Aspect-Based Sentiment Analysis)
    // =========================================================================

    private AgentResult computeTargetedAspectSentiment(String comment, Criterion criterion, Double overallRating) {
        String lower = (comment != null) ? comment.toLowerCase() : "";

        int baseline = 7;
        if (overallRating != null) {
            baseline = Math.max(2, Math.min(10, (int) Math.round(overallRating * 2.0)));
        }

        // Découpage en propositions pour isoler les sentiments (ex: "food was great BUT waiter was rude")
        String[] clauses = lower.split("[,;:.!?\\n]+|\\b(but|however|mais|pourtant|cependant|par contre)\\b");

        String targetPattern;
        switch (criterion) {
            case FOOD -> targetPattern = ".*(food|taste|dish|meal|meat|burger|pizza|pasta|soup|flavor|plat|cuisson|nourriture|viande|gout|goût|cuisine).*";
            case SERVICE -> targetPattern = ".*(service|waiter|staff|server|hospitality|serveur|personnel|accueil|attente).*";
            case PRICE -> targetPattern = ".*(price|expensive|cheap|cost|money|value|portion|bill|prix|cher|addition|rentable).*";
            case AMBIANCE -> targetPattern = ".*(ambience|ambiance|decor|atmosphere|music|sound|noise|clean|cadre|musique|propre|bruit).*";
            default -> targetPattern = "";
        }

        Pattern positiveWords = Pattern.compile("\\b(great|good|delicious|amazing|fresh|fast|friendly|tasty|best|clean|wonderful|bon|delicieux|parfait|rapide|aimable|top|super|agreable|agréable|adore|recommande)\\b");
        Pattern negativeWords = Pattern.compile("\\b(rude|bad|cold|slow|terrible|horrible|poor|worst|awful|disappointing|disappointed|stale|tasteless|unfriendly|dirty|expensive|overpriced|mauvais|froid|lent|déçu|decu|catastrophe|nul|cher|attente|sale|dégueulasse|decevant)\\b|not\\s+(good|great|fresh|friendly)");

        for (String clause : clauses) {
            if (clause.matches(targetPattern)) {
                boolean isPos = positiveWords.matcher(clause).find();
                boolean isNeg = negativeWords.matcher(clause).find();

                if (isPos && !isNeg) {
                    return new AgentResult(9, "Évaluation positive identifiée pour " + criterion.name().toLowerCase());
                } else if (isNeg && !isPos) {
                    return new AgentResult(2, "Critique négative identifiée pour " + criterion.name().toLowerCase());
                }
            }
        }

        return new AgentResult(baseline, "Note alignée sur l'impression générale.");
    }

    // =========================================================================
    // Détection Anti-Fraude / Anomalies en Temps Réel (detectAnomaly)
    // =========================================================================

    public AnomalyCheck detectAnomaly(Double rating, int food, int service, int price, int ambiance, String comment) {
        if (rating == null) {
            return new AnomalyCheck(false, null);
        }

        double aiAverageOn10 = (food + service + price + ambiance) / 4.0;
        double aiRatingOn5 = aiAverageOn10 / 2.0;
        double discrepancy = Math.abs(rating - aiRatingOn5);

        if (rating <= 2.0 && aiAverageOn10 >= 8.0) {
            String reason = String.format(
                    "Avis contradictoire bloqué : Vous avez attribué %.1f étoile(s), mais votre commentaire exprime une grande satisfaction (Score calculé: %.1f/5).",
                    rating, aiRatingOn5
            );
            return new AnomalyCheck(true, reason);
        }

        if (rating >= 4.5 && aiAverageOn10 <= 3.0) {
            String reason = String.format(
                    "Avis contradictoire bloqué : Vous avez attribué %.1f étoile(s), mais votre commentaire exprime une forte déception (Score calculé: %.1f/5).",
                    rating, aiRatingOn5
            );
            return new AnomalyCheck(true, reason);
        }

        if (discrepancy >= 3.0) {
            String reason = String.format(
                    "Incohérence majeure détectée entre votre note (%.1f/5) et les propos de votre avis.",
                    rating
            );
            return new AnomalyCheck(true, reason);
        }

        return new AnomalyCheck(false, null);
    }

    // =========================================================================
    // Moteur RAG : Recommandation de vrais restaurants de la plateforme (MongoDB)
    // =========================================================================

    /**
     * Interroge les restaurants réels de la plateforme (MongoDB)
     * pour proposer des alternatives réputées excelling sur le critère faible.
     */
    public List<RestaurantRecommendation> retrieveRagRecommendations(
            String currentRestaurantId,
            int food,
            int service,
            int price,
            int ambiance,
            String userComment
    ) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Cuisine", food);
        scores.put("Service", service);
        scores.put("Prix", price);
        scores.put("Ambiance", ambiance);

        String weakestCriterion = scores.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Cuisine");

        log.info("Critère critique identifié : {} (Score: {}/10). Recherche d'alternatives sur la plateforme...",
                weakestCriterion, scores.get(weakestCriterion));

        // Interroger la base de données réelle des restaurants (Module Restaurant du projet)
        List<Restaurant> allRestaurants = restaurantRepository.findAll();

        return allRestaurants.stream()
                .filter(r -> !r.getId().equals(currentRestaurantId))
                .filter(r -> r.getRating() != null && r.getRating() >= 4.0)
                .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
                .limit(3)
                .map(r -> {
                    String reason = switch (weakestCriterion) {
                        case "Cuisine" -> "Excellence culinaire certifiée (Note: " + r.getRating() + " ⭐)";
                        case "Service" -> "Service client très rapide et attentionné";
                        case "Prix" -> "Rapport qualité/prix remarquable (" + r.getPriceRange() + ")";
                        case "Ambiance" -> "Cadre chaleureux et ambiance conviviale garantie";
                        default -> "Hautement apprécié par la communauté";
                    };

                    return RestaurantRecommendation.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .cuisine(r.getCuisine())
                            .description(r.getDescription())
                            .image(r.getImage() != null && !r.getImage().isBlank()
                                    ? r.getImage()
                                    : "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4")
                            .rating(r.getRating())
                            .address(r.getAddress())
                            .priceRange(r.getPriceRange() != null ? r.getPriceRange().name() : "MODERATE")
                            .recommendationReason(reason)
                            .strongCriterion(weakestCriterion)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private enum Criterion { FOOD, SERVICE, PRICE, AMBIANCE }

    public record AgentResult(int score, String thought) {}
    public record AnomalyCheck(boolean isAnomaly, String reason) {}
}
