package com.hangout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.dto.GameStatusResponse;
import com.hangout.dto.LoyaltyOverview;
import com.hangout.dto.RecommendationResponse;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.model.RewardType;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Suggests where a client can get more out of their points, from their reservation history,
 * the places they visited and their points per restaurant. Gemini writes the advice; the server
 * checks every suggestion against real data (ids, costs, balances) and falls back to
 * built-in rules when the model is not configured or fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private static final int MAX_SUGGESTIONS = 5;
    private static final long CACHE_MILLIS = 10 * 60 * 1000L;

    private final ReservationRepository reservationRepository;
    private final RestaurantRepository restaurantRepository;
    private final LoyaltyService loyaltyService;
    private final GeminiClient geminiClient;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(String fingerprint, long at, RecommendationResponse response) {}

    public RecommendationResponse recommend(String userId) {
        LoyaltyOverview overview = loyaltyService.getOverview(userId);
        Map<String, Restaurant> restaurants = restaurantRepository.findAll().stream()
            .collect(Collectors.toMap(Restaurant::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
        List<Reservation> reservations = reservationRepository.findByUserId(userId);

        Map<String, Object> context = buildContext(overview, restaurants, reservations);
        String fingerprint;
        try {
            fingerprint = Integer.toHexString(mapper.writeValueAsString(context).hashCode());
        } catch (Exception e) {
            fingerprint = String.valueOf(System.nanoTime());
        }

        Cached cached = cache.get(userId);
        if (cached != null && cached.fingerprint().equals(fingerprint) && System.currentTimeMillis() - cached.at() < CACHE_MILLIS) {
            return cached.response();
        }

        RecommendationResponse response;
        if (!geminiClient.isConfigured()) {
            response = fallback(overview, restaurants, reservations);
            response.setNotice("Gemini is not configured on the server, so these suggestions come from built-in rules.");
        } else {
            try {
                response = askGemini(context, overview, restaurants);
            } catch (Exception e) {
                log.warn("Gemini recommendation failed, using fallback: {}", e.getMessage());
                response = fallback(overview, restaurants, reservations);
                response.setNotice("Gemini is not available right now, so these suggestions come from built-in rules.");
            }
        }

        // Only cache real answers (a transient failure should be retried on the next click).
        if ("GEMINI".equals(response.getSource())) {
            cache.put(userId, new Cached(fingerprint, System.currentTimeMillis(), response));
        }
        return response;
    }

    // ------------------------------------------------------------------ context

    private Map<String, Object> buildContext(LoyaltyOverview overview, Map<String, Restaurant> restaurants,
                                             List<Reservation> reservations) {
        DateTimeFormatter day = DateTimeFormatter.ISO_LOCAL_DATE;
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("totalPoints", overview.getTotalPoints());
        // Old points not tied to any restaurant count for the level but cannot be exchanged anywhere.
        ctx.put("pointsNotTiedToAnyRestaurant", overview.getUnassignedPoints());

        ctx.put("rewardsCatalogue", java.util.Arrays.stream(RewardType.values()).map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", t.name());
            m.put("title", t.getTitle());
            m.put("costInPoints", t.getCost());
            return m;
        }).collect(Collectors.toList()));

        // Points per restaurant (only where it matters) with loyalty on/off.
        ctx.put("pointsPerRestaurant", overview.getRestaurants().stream()
            .filter(r -> r.getPoints() > 0 || !r.isLoyaltyEnabled())
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("restaurantId", r.getRestaurantId());
                m.put("name", r.getName());
                m.put("points", r.getPoints());
                m.put("loyaltyEnabled", r.isLoyaltyEnabled());
                return m;
            }).collect(Collectors.toList()));

        // History: most recent 20 reservations.
        List<Reservation> recent = reservations.stream()
            .sorted(Comparator.comparing(Reservation::getReservationDateTime,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(20)
            .collect(Collectors.toList());
        ctx.put("reservationHistory", recent.stream().map(r -> {
            Restaurant rest = restaurants.get(r.getRestaurantId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("restaurant", rest != null ? rest.getName() : "unknown");
            m.put("cuisine", rest != null ? rest.getCuisine() : "");
            m.put("date", r.getReservationDateTime() != null ? r.getReservationDateTime().format(day) : "");
            m.put("kind", r.getEventId() != null ? "event" : "table");
            m.put("status", r.getStatus() != null ? r.getStatus().name() : "");
            m.put("guests", r.getPartySize());
            return m;
        }).collect(Collectors.toList()));

        // Places really visited (arrived / completed), with counts.
        Map<String, Long> visits = reservations.stream()
            .filter(r -> r.getStatus() == Reservation.ReservationStatus.ARRIVED
                || r.getStatus() == Reservation.ReservationStatus.COMPLETED)
            .collect(Collectors.groupingBy(Reservation::getRestaurantId, Collectors.counting()));
        ctx.put("placesVisited", visits.entrySet().stream().map(e -> {
            Restaurant rest = restaurants.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("restaurantId", e.getKey());
            m.put("name", rest != null ? rest.getName() : "unknown");
            m.put("cuisine", rest != null ? rest.getCuisine() : "");
            m.put("visits", e.getValue());
            return m;
        }).collect(Collectors.toList()));

        // Restaurants the user could pick from.
        ctx.put("restaurants", restaurants.values().stream().limit(30).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("restaurantId", r.getId());
            m.put("name", r.getName());
            m.put("cuisine", r.getCuisine());
            m.put("priceRange", r.getPriceRange() != null ? r.getPriceRange().name() : "");
            m.put("rating", r.getRating());
            m.put("loyaltyEnabled", !Boolean.FALSE.equals(r.getLoyaltyEnabled()));
            return m;
        }).collect(Collectors.toList()));

        boolean playedToday = overview.getGames().stream().anyMatch(GameStatusResponse::isPlayedToday);
        ctx.put("dailyGamePlayedToday", playedToday);
        return ctx;
    }

    // ------------------------------------------------------------------ Gemini

    private RecommendationResponse askGemini(Map<String, Object> context, LoyaltyOverview overview,
                                             Map<String, Restaurant> restaurants) throws Exception {
        String prompt = """
            You are the loyalty assistant of HangOut, a restaurant app. A client taps "Recommend".
            Using ONLY the JSON data below, tell them how to get the most from their points and where to go next.
            Points are earned and spent per restaurant: points at one restaurant can only be exchanged there,
            and restaurants with loyaltyEnabled=false cannot be used for rewards or points.
            pointsNotTiedToAnyRestaurant are old points that cannot be exchanged: do not count them when talking
            about what the client can afford, and base exchange advice only on pointsPerRestaurant.

            Rules:
            - Give 3 to 5 suggestions, best first. Be specific, warm and short.
            - type must be one of: EXCHANGE (the client can afford a reward now at a restaurant),
              SAVE (the client is close to a reward and should earn a bit more there),
              VISIT (a restaurant to try, based on cuisines and places visited, with loyaltyEnabled=true),
              PLAY (suggest the daily game, only if dailyGamePlayedToday is false, naming the restaurant to play for).
            - restaurantId must be copied exactly from the data. rewardType (EXCHANGE and SAVE only) must be one of the
              catalogue types. Never invent restaurants, rewards or numbers.
            - Prefer EXCHANGE where the points give the best value (a bigger reward the client can afford),
              and mention what they enjoy (cuisines they visited) when relevant.
            - "title" is at most 8 words. "reason" is one or two sentences explaining why it fits this client.
            - "summary" is one friendly sentence about their situation.
            - Answer in English.

            Return JSON exactly like:
            {"summary":"...","suggestions":[{"type":"EXCHANGE","title":"...","reason":"...","restaurantId":"...","rewardType":"FREE_COFFEE"}]}

            DATA:
            """ + mapper.writeValueAsString(context);

        JsonNode answer = geminiClient.generateJson(prompt);

        Map<String, LoyaltyOverview.RestaurantLoyalty> loyalty = overview.getRestaurants().stream()
            .collect(Collectors.toMap(LoyaltyOverview.RestaurantLoyalty::getRestaurantId, r -> r, (a, b) -> a));

        List<RecommendationResponse.Suggestion> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean playedToday = overview.getGames().stream().anyMatch(GameStatusResponse::isPlayedToday);

        for (JsonNode node : answer.path("suggestions")) {
            if (suggestions.size() >= MAX_SUGGESTIONS) break;
            String type = node.path("type").asText("").toUpperCase();
            String restaurantId = node.path("restaurantId").asText("");
            String rewardType = node.path("rewardType").asText("").toUpperCase();
            Restaurant restaurant = restaurants.get(restaurantId);
            LoyaltyOverview.RestaurantLoyalty rl = loyalty.get(restaurantId);

            RecommendationResponse.Suggestion s = new RecommendationResponse.Suggestion();
            s.setTitle(clip(node.path("title").asText(""), 80));
            s.setReason(clip(node.path("reason").asText(""), 320));
            if (s.getTitle().isBlank() || s.getReason().isBlank()) continue;

            switch (type) {
                case "EXCHANGE", "SAVE" -> {
                    RewardType reward = parseReward(rewardType);
                    if (restaurant == null || rl == null || reward == null || !rl.isLoyaltyEnabled()) continue;
                    fillReward(s, restaurant, rl, reward);
                    // The model says "exchange" but the client cannot afford it: show it as a goal instead.
                    s.setType(s.isCanRedeem() ? "EXCHANGE" : "SAVE");
                }
                case "VISIT" -> {
                    if (restaurant == null || Boolean.FALSE.equals(restaurant.getLoyaltyEnabled())) continue;
                    s.setType("VISIT");
                    fillRestaurant(s, restaurant);
                }
                case "PLAY" -> {
                    if (playedToday || restaurant == null || rl == null || !rl.isLoyaltyEnabled()) continue;
                    s.setType("PLAY");
                    fillRestaurant(s, restaurant);
                }
                default -> {
                    continue;
                }
            }

            String key = s.getType() + "|" + s.getRestaurantId() + "|" + s.getRewardType();
            if (seen.add(key)) suggestions.add(s);
        }

        if (suggestions.isEmpty()) {
            throw new IllegalStateException("Gemini returned no usable suggestion");
        }

        RecommendationResponse response = new RecommendationResponse();
        response.setSource("GEMINI");
        response.setSummary(clip(answer.path("summary").asText(""), 240));
        if (response.getSummary().isBlank()) response.setSummary("Here is how to get more from your points.");
        response.setSuggestions(suggestions);
        return response;
    }

    // ------------------------------------------------------------------ fallback rules

    private RecommendationResponse fallback(LoyaltyOverview overview, Map<String, Restaurant> restaurants,
                                            List<Reservation> reservations) {
        List<RecommendationResponse.Suggestion> out = new ArrayList<>();
        RewardType[] byValue = RewardType.values();

        List<LoyaltyOverview.RestaurantLoyalty> withPoints = overview.getRestaurants().stream()
            .filter(r -> r.isLoyaltyEnabled() && r.getPoints() > 0)
            .sorted(Comparator.comparingInt(LoyaltyOverview.RestaurantLoyalty::getPoints).reversed())
            .collect(Collectors.toList());

        // 1. Best reward the client can afford right now, per restaurant.
        for (LoyaltyOverview.RestaurantLoyalty rl : withPoints) {
            RewardType best = null;
            for (RewardType t : byValue) {
                if (rl.getPoints() >= t.getCost() && (best == null || t.getCost() > best.getCost())) best = t;
            }
            Restaurant restaurant = restaurants.get(rl.getRestaurantId());
            if (best != null && restaurant != null && out.size() < 2) {
                RecommendationResponse.Suggestion s = new RecommendationResponse.Suggestion();
                s.setType("EXCHANGE");
                s.setTitle("Get " + best.getTitle().toLowerCase() + " at " + restaurant.getName());
                s.setReason("You have " + rl.getPoints() + " points at " + restaurant.getName()
                    + ", enough for the best reward you can afford there.");
                fillReward(s, restaurant, rl, best);
                out.add(s);
            }
        }

        // 2. The reward closest to being affordable.
        RecommendationResponse.Suggestion closest = null;
        for (LoyaltyOverview.RestaurantLoyalty rl : withPoints) {
            Restaurant restaurant = restaurants.get(rl.getRestaurantId());
            if (restaurant == null) continue;
            for (RewardType t : byValue) {
                int missing = t.getCost() - rl.getPoints();
                if (missing > 0 && (closest == null || missing < closest.getMissingPoints())) {
                    RecommendationResponse.Suggestion s = new RecommendationResponse.Suggestion();
                    s.setType("SAVE");
                    s.setTitle("Almost there: " + t.getTitle().toLowerCase());
                    s.setReason("Only " + missing + " more points at " + restaurant.getName() + " to unlock it.");
                    fillReward(s, restaurant, rl, t);
                    closest = s;
                }
            }
        }
        if (closest != null) out.add(closest);

        // 3. Places to try, based on the cuisines of the places visited.
        Set<String> visitedIds = reservations.stream()
            .filter(r -> r.getStatus() == Reservation.ReservationStatus.ARRIVED
                || r.getStatus() == Reservation.ReservationStatus.COMPLETED)
            .map(Reservation::getRestaurantId).collect(Collectors.toSet());
        Map<String, Long> cuisineCount = visitedIds.stream()
            .map(restaurants::get).filter(r -> r != null && r.getCuisine() != null)
            .collect(Collectors.groupingBy(Restaurant::getCuisine, Collectors.counting()));
        String favourite = cuisineCount.entrySet().stream().max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);

        List<Restaurant> toTry = restaurants.values().stream()
            .filter(r -> !visitedIds.contains(r.getId()) && !Boolean.FALSE.equals(r.getLoyaltyEnabled()))
            .sorted(Comparator
                .comparing((Restaurant r) -> favourite != null && favourite.equalsIgnoreCase(r.getCuisine()) ? 0 : 1)
                .thenComparing(Comparator.comparing((Restaurant r) -> r.getRating() == null ? 0.0 : r.getRating()).reversed()))
            .limit(2).collect(Collectors.toList());
        for (Restaurant r : toTry) {
            if (out.size() >= MAX_SUGGESTIONS) break;
            RecommendationResponse.Suggestion s = new RecommendationResponse.Suggestion();
            s.setType("VISIT");
            s.setTitle("Try " + r.getName());
            s.setReason(favourite != null && favourite.equalsIgnoreCase(r.getCuisine())
                ? "You enjoy " + favourite + " food and you have not been here yet."
                : "A well-rated place with a loyalty programme where you can start earning points.");
            fillRestaurant(s, r);
            out.add(s);
        }

        // 4. The daily game, played where it helps most.
        boolean playedToday = overview.getGames().stream().anyMatch(GameStatusResponse::isPlayedToday);
        if (!playedToday && out.size() < MAX_SUGGESTIONS) {
            LoyaltyOverview.RestaurantLoyalty target = !withPoints.isEmpty() ? withPoints.get(0)
                : overview.getRestaurants().stream().filter(LoyaltyOverview.RestaurantLoyalty::isLoyaltyEnabled)
                    .findFirst().orElse(null);
            Restaurant restaurant = target != null ? restaurants.get(target.getRestaurantId()) : null;
            if (restaurant != null) {
                RecommendationResponse.Suggestion s = new RecommendationResponse.Suggestion();
                s.setType("PLAY");
                s.setTitle("Play today's game for " + restaurant.getName());
                s.setReason("You have not played today. Playing for the same restaurant brings you closer to a reward.");
                fillRestaurant(s, restaurant);
                out.add(s);
            }
        }

        RecommendationResponse response = new RecommendationResponse();
        response.setSource("FALLBACK");
        response.setSuggestions(out.stream().limit(MAX_SUGGESTIONS).collect(Collectors.toList()));
        response.setSummary(overview.getTotalPoints() > 0
            ? "You have " + overview.getTotalPoints() + " points. Here is how to use them well."
            : "Start collecting points: play the daily game or book a table.");
        return response;
    }

    // ------------------------------------------------------------------ helpers

    private void fillRestaurant(RecommendationResponse.Suggestion s, Restaurant restaurant) {
        s.setRestaurantId(restaurant.getId());
        s.setRestaurantName(restaurant.getName());
        s.setRestaurantImage(restaurant.getImage() == null ? "" : restaurant.getImage());
    }

    /** Numbers shown to the client always come from the real balance, never from the model. */
    private void fillReward(RecommendationResponse.Suggestion s, Restaurant restaurant,
                            LoyaltyOverview.RestaurantLoyalty rl, RewardType reward) {
        fillRestaurant(s, restaurant);
        s.setRewardType(reward.name());
        s.setRewardTitle(reward.getTitle());
        s.setCost(reward.getCost());
        s.setRestaurantPoints(rl.getPoints());
        s.setCanRedeem(rl.isLoyaltyEnabled() && rl.getPoints() >= reward.getCost());
        s.setMissingPoints(Math.max(0, reward.getCost() - rl.getPoints()));
    }

    private static RewardType parseReward(String value) {
        try {
            return RewardType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String clip(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() > max ? v.substring(0, max - 1).trim() + "…" : v;
    }
}
