package com.hangout.ai.service;

import com.hangout.ai.client.EmbeddingService;
import com.hangout.ai.model.Dish;
import com.hangout.ai.model.DishChunk;
import com.hangout.ai.repository.DishChunkRepository;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class IndexingService {
    private final MenuExtractor extractor;
    private final EmbeddingService embeddings;
    private final DishChunkRepository chunkRepo;
    private final RestaurantRepository restaurants;

    public IndexingService(MenuExtractor extractor, EmbeddingService embeddings,
                           DishChunkRepository chunkRepo, RestaurantRepository restaurants) {
        this.extractor = extractor;
        this.embeddings = embeddings;
        this.chunkRepo = chunkRepo;
        this.restaurants = restaurants;
    }

    public int indexRestaurant(Restaurant r, byte[] menuImage, String mediaType) throws Exception {
        List<Dish> dishes = extractor.extract(menuImage, mediaType);
        chunkRepo.deleteByRestaurantId(r.getId());
        for (Dish d : dishes) {
            String text = d.name() + " : " + Objects.toString(d.description(), "")
                + ". Catégorie " + Objects.toString(d.category(), "") + ". Cuisine "
                + Objects.toString(r.getCuisine(), "") + ".";
            chunkRepo.save(new DishChunk(null, r.getId(), r.getName(),
                d.name(), text, d.price(), embeddings.embed(text)));
            Thread.sleep(300);
        }
        syncSharedMenus();
        return dishes.size();
    }

    public Map<String, Object> indexStoredMenu(Restaurant r) throws Exception {
        String menu = r.getMenuImage();
        if (menu == null || menu.isBlank()) {
            return Map.of("restaurant", String.valueOf(r.getName()), "skipped", "pas de menuImage");
        }

        Restaurant twin = findIndexedTwin(r);
        if (twin != null) {
            int copied = copyChunks(twin, r);
            return Map.of(
                "restaurant", String.valueOf(r.getName()),
                "dishesIndexed", copied,
                "copiedFrom", String.valueOf(twin.getName())
            );
        }

        Path file = resolveMenuFile(r.getMenuImage());
        if (file == null || !Files.exists(file)) {
            return Map.of("restaurant", String.valueOf(r.getName()), "skipped",
                "fichier introuvable: " + menuFileName(r.getMenuImage()));
        }

        String fileName = menuFileName(r.getMenuImage());
        String type = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        int n = indexRestaurant(r, Files.readAllBytes(file), type);
        return Map.of("restaurant", String.valueOf(r.getName()), "dishesIndexed", n);
    }

    public boolean hasIndexedMenu(String restaurantId) {
        return restaurantId != null && !chunkRepo.findByRestaurantId(restaurantId).isEmpty();
    }

    public void indexStoredMenuAsync(Restaurant r) {
        if (r == null || r.getId() == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                restaurants.findById(r.getId()).ifPresent(fresh -> {
                    try {
                        indexStoredMenu(fresh);
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        });
    }

    public int syncSharedMenus() {
        int copied = 0;
        List<Restaurant> all = restaurants.findAll();
        for (Restaurant target : all) {
            if (target.getId() == null || target.getMenuImage() == null || target.getMenuImage().isBlank()) {
                continue;
            }
            Restaurant source = findIndexedTwin(target);
            if (source == null) {
                continue;
            }
            int sourceSize = chunkRepo.findByRestaurantId(source.getId()).size();
            int targetSize = chunkRepo.findByRestaurantId(target.getId()).size();
            if (sourceSize > targetSize) {
                copied += copyChunks(source, target);
            }
        }
        return copied;
    }

    private Restaurant findIndexedTwin(Restaurant r) {
        String fileName = menuFileName(r.getMenuImage());
        String hash = menuFileHash(r.getMenuImage());
        return restaurants.findAll().stream()
            .filter(other -> other.getId() != null && !other.getId().equals(r.getId()))
            .filter(other -> !chunkRepo.findByRestaurantId(other.getId()).isEmpty())
            .filter(other -> sameMenu(fileName, hash, other))
            .max(Comparator.comparingInt(other -> chunkRepo.findByRestaurantId(other.getId()).size()))
            .orElse(null);
    }

    private boolean sameMenu(String fileName, String hash, Restaurant other) {
        String otherName = menuFileName(other.getMenuImage());
        if (!fileName.isBlank() && fileName.equals(otherName)) {
            return true;
        }
        String otherHash = menuFileHash(other.getMenuImage());
        return !hash.isBlank() && hash.equals(otherHash);
    }

    private int copyChunks(Restaurant from, Restaurant to) {
        chunkRepo.deleteByRestaurantId(to.getId());
        List<DishChunk> source = chunkRepo.findByRestaurantId(from.getId());
        for (DishChunk chunk : source) {
            chunkRepo.save(new DishChunk(
                null,
                to.getId(),
                to.getName(),
                chunk.getDishName(),
                chunk.getText(),
                chunk.getPrice(),
                chunk.getEmbedding()
            ));
        }
        return source.size();
    }

    private static Path resolveMenuFile(String menuImage) {
        String fileName = menuFileName(menuImage);
        if (fileName.isBlank()) {
            return null;
        }
        return Paths.get("uploads", "menus", fileName);
    }

    private static String menuFileName(String menuImage) {
        if (menuImage == null || menuImage.isBlank()) {
            return "";
        }
        return menuImage.substring(menuImage.lastIndexOf('/') + 1);
    }

    private static String menuFileHash(String menuImage) {
        try {
            Path file = resolveMenuFile(menuImage);
            if (file == null || !Files.exists(file)) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (Exception e) {
            return "";
        }
    }
}
