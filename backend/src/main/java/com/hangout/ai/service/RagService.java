package com.hangout.ai.service;

import com.hangout.ai.client.EmbeddingService;
import com.hangout.ai.model.DishChunk;
import com.hangout.ai.repository.DishChunkRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RagService {
    private static final double MIN_SCORE = 0.50;   // seuil à ajuster après tests

    private final EmbeddingService embeddings;
    private final DishChunkRepository chunkRepo;

    public RagService(EmbeddingService embeddings, DishChunkRepository chunkRepo) {
        this.embeddings = embeddings;
        this.chunkRepo = chunkRepo;
    }

    public List<DishChunk> retrieve(String query, int k) {
        List<Double> q = embeddings.embed(query);
        List<Scored> scored = chunkRepo.findAll().stream()
            .filter(c -> c.getEmbedding() != null && c.getEmbedding().size() == q.size())
            .map(c -> new Scored(c, cosine(q, c.getEmbedding())))
            .filter(s -> s.score > MIN_SCORE)
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .toList();

        Set<String> topDishNames = new LinkedHashSet<>();
        for (Scored scoredDish : scored) {
            String key = normalizeDishName(scoredDish.chunk.getDishName());
            if (!key.isBlank()) {
                topDishNames.add(key);
            }
            if (topDishNames.size() >= k) {
                break;
            }
        }

        return scored.stream()
            .filter(s -> topDishNames.contains(normalizeDishName(s.chunk.getDishName())))
            .map(s -> s.chunk)
            .toList();
    }

    public List<DishChunk> retrieve(String query, int k, Double maxPrice) {
        List<DishChunk> hits = retrieve(query, k);
        if (maxPrice == null) {
            return hits;
        }
        return hits.stream()
            .filter(c -> c.getPrice() != null && c.getPrice() <= maxPrice)
            .sorted(Comparator.comparing(DishChunk::getPrice))
            .toList();
    }

    private static String normalizeDishName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private record Scored(DishChunk chunk, double score) {}

    static double cosine(List<Double> a, List<Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}