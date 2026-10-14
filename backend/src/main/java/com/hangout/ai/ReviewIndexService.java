package com.hangout.ai;

import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.ReviewChunk;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewChunkRepository;
import com.hangout.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewIndexService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    private volatile long lastCheck = 0;

    public record SyncResult(int reviews, int indexed, int removed) {}

    /** Synchronise l'index avec les avis : ajoute les nouveaux, met à jour les modifiés, retire les supprimés. */
    public synchronized SyncResult sync() {
        Map<String, Restaurant> restaurants = new HashMap<>();
        for (Restaurant r : restaurantRepository.findAll()) {
            if (r.getId() != null) restaurants.put(r.getId(), r);
        }

        Map<String, Review> valid = new LinkedHashMap<>();
        Map<String, String> enriched = new HashMap<>();
        for (Review r : reviewRepository.findAll()) {
            if (r.getId() == null || r.getRestaurantId() == null) continue;
            String status = String.valueOf(r.getStatus());
            if (status.equals("DELETED") || status.equals("REJECTED")) continue;
            String body = bodyOf(r);
            if (body.isBlank()) continue;
            valid.put(r.getId(), r);
            enriched.put(r.getId(), enrich(r, restaurants.get(r.getRestaurantId()), body));
        }

        Map<String, ReviewChunk> existing = new HashMap<>();
        List<ReviewChunk> obsolete = new ArrayList<>();
        for (ReviewChunk c : chunkRepository.findAll()) {
            if (valid.containsKey(c.getReviewId())) existing.put(c.getReviewId(), c);
            else obsolete.add(c);
        }

        List<String> idsToIndex = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (String reviewId : valid.keySet()) {
            String hash = sha256(enriched.get(reviewId));
            ReviewChunk ex = existing.get(reviewId);
            if (ex == null || !hash.equals(ex.getTextHash())) {
                idsToIndex.add(reviewId);
                texts.add(enriched.get(reviewId));
            }
        }

        List<ReviewChunk> toSave = new ArrayList<>();
        if (!texts.isEmpty()) {
            List<double[]> vectors = embeddingClient.embedDocuments(texts);
            for (int i = 0; i < idsToIndex.size(); i++) {
                Review r = valid.get(idsToIndex.get(i));
                ReviewChunk c = existing.getOrDefault(r.getId(), new ReviewChunk());
                c.setReviewId(r.getId());
                c.setRestaurantId(r.getRestaurantId());
                c.setRating(ratingOf(r));
                c.setText(bodyOf(r));
                c.setTextHash(sha256(texts.get(i)));
                c.setEmbedding(vectors.get(i));
                c.setIndexedAt(LocalDateTime.now());
                toSave.add(c);
            }
        }

        chunkRepository.deleteAll(obsolete);
        chunkRepository.saveAll(toSave);
        lastCheck = System.currentTimeMillis();
        return new SyncResult(valid.size(), toSave.size(), obsolete.size());
    }

    /** Appelé avant chaque recherche : resynchronise au plus toutes les 10 secondes. */
    public void ensureIndexed() {
        if (System.currentTimeMillis() - lastCheck < 10_000) return;
        sync();
    }

    // Texte nettoyé de l'avis (titre + commentaire)
    private static String bodyOf(Review r) {
        String title = r.getTitle() == null ? "" : r.getTitle().trim();
        String comment = r.getComment() == null ? "" : r.getComment().trim();
        String joined = title.isEmpty() ? comment : (comment.isEmpty() ? title : title + ". " + comment);
        return ReviewTextSanitizer.clean(joined, 600);
    }

    // Texte enrichi du contexte du restaurant, utilisé uniquement pour l'embedding
    private static String enrich(Review r, Restaurant rest, String body) {
        String name = rest != null && rest.getName() != null ? rest.getName() : "";
        String cuisine = rest != null && rest.getCuisine() != null ? rest.getCuisine() : "";
        return "Restaurant " + name + " (cuisine " + cuisine + "), note "
                + ratingOf(r) + "/5. " + body;
    }

    private static double ratingOf(Review r) {
        Object o = r.getRating();
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}