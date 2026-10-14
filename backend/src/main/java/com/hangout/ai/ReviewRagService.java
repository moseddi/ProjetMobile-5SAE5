package com.hangout.ai;

import com.hangout.model.Event;
import com.hangout.model.Restaurant;
import com.hangout.model.ReviewChunk;
import com.hangout.repository.EventRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRagService {

    private static final int TOP_K = 10;
    private static final int SNIPPETS_PER_EVENT = 3;
    private static final int MAX_EVENTS_PER_RESTAURANT = 2;
    private static final String DEFAULT_QUERY =
            "excellente expérience, accueil chaleureux, cuisine délicieuse";

    private final EventRepository eventRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewChunkRepository chunkRepository;
    private final ReviewIndexService indexService;
    private final EmbeddingClient embeddingClient;
    private final RecommendationService recommendationService;
    private final RagAnswerGenerator answerGenerator;

    private record Hit(ReviewChunk chunk, double sim) {}
    private record Cand(String restaurantId, double score, double avgRating, List<Hit> hits) {}
    private record Query(String text, String mode) {}

    public RagResponse recommend(RagRequest req) {
        int limit = Math.max(1, Math.min(req.limit() == null ? 4 : req.limit(), 6));

        // Événements candidats : à venir et non complets
        List<Event> upcoming = eventRepository.findByEventDateAfter(LocalDateTime.now()).stream()
                .filter(e -> e.getRestaurantId() != null && spotsLeft(e) > 0)
                .sorted(Comparator.comparing(Event::getEventDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (upcoming.isEmpty()) {
            return empty("", "NONE", "Aucun événement à venir pour le moment.");
        }

        indexService.ensureIndexed();

        // ---------- 1) Requête : texte libre, profil du client, ou requête par défaut ----------
        Query q = resolveQuery(req);

        // ---------- 2) RETRIEVAL : recherche sémantique dans les avis ----------
        Set<String> candidateRestaurants = upcoming.stream()
                .map(Event::getRestaurantId).collect(Collectors.toSet());
        double[] queryVector = embeddingClient.embedQuery(q.text());

        List<Hit> hits = chunkRepository.findAll().stream()
                .filter(c -> c.getEmbedding() != null
                        && candidateRestaurants.contains(c.getRestaurantId()))
                .map(c -> new Hit(c, VectorMath.cosine(queryVector, c.getEmbedding())))
                .filter(h -> h.sim() > 0)
                .sorted(Comparator.comparingDouble(Hit::sim).reversed())
                .limit(TOP_K)
                .toList();
        if (hits.isEmpty()) {
            return empty(q.text(), q.mode(),
                    "Aucun avis pertinent trouvé. Ajoutez des avis ou reformulez votre demande.");
        }

        // Regroupement par restaurant, puis score = 70 % pertinence + 30 % note moyenne
        Map<String, List<Hit>> grouped = new LinkedHashMap<>();
        for (Hit h : hits) {
            grouped.computeIfAbsent(h.chunk().getRestaurantId(), k -> new ArrayList<>()).add(h);
        }
        List<Cand> candidates = grouped.entrySet().stream().map(en -> {
            List<Hit> hs = en.getValue();
            double avg = hs.stream().mapToDouble(h -> ratingOf(h.chunk())).average().orElse(0);
            double score = 0.7 * hs.get(0).sim() + 0.3 * (avg / 5.0);
            return new Cand(en.getKey(), score, avg, hs);
        }).sorted(Comparator.comparingDouble(Cand::score).reversed()).toList();

        // ---------- 3) Sélection des événements et des extraits d'avis ----------
        Map<String, Restaurant> restaurants = new HashMap<>();
        for (Restaurant r : restaurantRepository.findAll()) {
            if (r.getId() != null) restaurants.put(r.getId(), r);
        }
        Map<String, List<Event>> eventsByRestaurant = upcoming.stream()
                .collect(Collectors.groupingBy(Event::getRestaurantId, LinkedHashMap::new,
                        Collectors.toList()));

        List<RagPick> picks = new ArrayList<>();
        int label = 1;
        outer:
        for (Cand c : candidates) {
            int taken = 0;
            for (Event e : eventsByRestaurant.getOrDefault(c.restaurantId(), List.of())) {
                if (taken >= MAX_EVENTS_PER_RESTAURANT) break;
                if (picks.size() >= limit) break outer;

                List<ReviewSnippet> snippets = new ArrayList<>();
                for (Hit h : c.hits().stream().limit(SNIPPETS_PER_EVENT).toList()) {
                    snippets.add(new ReviewSnippet("R" + label++, ratingOf(h.chunk()),
                            truncate(h.chunk().getText(), 260), percent(h.sim())));
                }
                Restaurant r = restaurants.get(c.restaurantId());
                String name = r != null && r.getName() != null ? r.getName() : "Restaurant";
                picks.add(new RagPick(e, name, c.avgRating(), c.score(), snippets));
                taken++;
            }
        }

        // ---------- 4) GENERATION : explication par le LLM, fondée sur les avis ----------
        Map<Integer, RagAnswerGenerator.Answer> answers = Map.of();
        try {
            answers = answerGenerator.generate(q.text(), picks);
        } catch (AiException ex) {
            // L'IA est en panne ou sans quota : on renvoie quand même les résultats de la recherche
            log.warn("Génération RAG indisponible : {}", ex.getMessage());
        }

        List<RagItem> items = new ArrayList<>();
        for (int i = 0; i < picks.size(); i++) {
            RagPick p = picks.get(i);
            RagAnswerGenerator.Answer a = answers.get(i + 1);
            String explanation = a != null ? a.text() : fallback(p);
            items.add(new RagItem(p.event(), p.restaurantName(),
                    Math.round(p.avgRating() * 10) / 10.0,
                    percent(p.score()), explanation, p.snippets()));
        }
        return new RagResponse(q.text(), q.mode(), !answers.isEmpty(), items, null);
    }

    private Query resolveQuery(RagRequest req) {
        String typed = ReviewTextSanitizer.clean(req.query(), 300);
        if (!typed.isBlank()) return new Query(typed, "QUERY");

        List<String> liked = likedCuisines(req.userId());
        if (!liked.isEmpty()) {
            return new Query("Avis positifs sur une cuisine " + String.join(", ", liked)
                    + " et une bonne ambiance", "PROFILE");
        }
        return new Query(DEFAULT_QUERY, "DEFAULT");
    }

    private List<String> likedCuisines(String userId) {
        if (userId == null || userId.isBlank()) return List.of();
        try {
            return recommendationService.recommend(userId, 1).likedCuisines();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    // Repli sans LLM : l'avis le plus pertinent, cité tel quel
    private static String fallback(RagPick p) {
        ReviewSnippet best = p.snippets().get(0);
        return "Avis le plus pertinent : « " + truncate(best.text(), 140) + " »";
    }

    private static RagResponse empty(String query, String mode, String message) {
        return new RagResponse(query, mode, false, List.of(), message);
    }

    private static int spotsLeft(Event e) {
        int total = e.getAvailableSpots() == null ? 0 : e.getAvailableSpots();
        int booked = e.getBookedSpots() == null ? 0 : e.getBookedSpots();
        return total - booked;
    }

    private static double ratingOf(ReviewChunk c) {
        return c.getRating() == null ? 0.0 : c.getRating();
    }

    private static int percent(double v) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, v)) * 100);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}