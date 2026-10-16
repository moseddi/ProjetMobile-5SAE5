package com.hangout.service;

import com.hangout.ai.service.IndexingService;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final RestaurantRepository restaurantRepository;
    private final IndexingService indexingService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }
    
    public Restaurant getRestaurantById(String id) {
        return restaurantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }
    
    public List<Restaurant> getRestaurantsByCuisine(String cuisine) {
        return restaurantRepository.findByCuisine(cuisine);
    }
    
    public Restaurant createRestaurant(Restaurant restaurant) {
        Restaurant saved = restaurantRepository.save(restaurant);
        indexingService.indexStoredMenuAsync(saved);
        return saved;
    }
    
    public Restaurant updateRestaurant(String id, Restaurant restaurant) {
        Restaurant existing = getRestaurantById(id);
        restaurant.setId(id);
        restaurant.setCreatedAt(existing.getCreatedAt());
        if (restaurant.getMenuImage() == null) {
            restaurant.setMenuImage(existing.getMenuImage());
        }
        Restaurant saved = restaurantRepository.save(restaurant);
        if (!java.util.Objects.equals(existing.getMenuImage(), saved.getMenuImage())
                || !indexingService.hasIndexedMenu(saved.getId())) {
            indexingService.indexStoredMenuAsync(saved);
        }
        return saved;
    }
    
    public void deleteRestaurant(String id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + id);
        }
        restaurantRepository.deleteById(id);
    }

    public String uploadMenuImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a menu image");
        }

        String extension = resolveImageExtension(file);
        if (extension == null) {
            throw new BadRequestException("Menu must be an image (JPG, PNG, WEBP or GIF)");
        }

        try {
            Path menuDir = getMenuDirectory();
            Files.createDirectories(menuDir);
            String filename = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), menuDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return "/api/restaurants/menu-files/" + filename;
        } catch (IOException exception) {
            throw new BadRequestException("Failed to save menu image");
        }
    }

    public Resource getMenuImage(String filename) {
        if (filename == null || !filename.matches("[a-fA-F0-9\\-]+\\.(jpg|jpeg|png|webp|gif)")) {
            throw new BadRequestException("Invalid menu image");
        }
        Path file = getMenuDirectory().resolve(filename).normalize();
        if (!file.startsWith(getMenuDirectory()) || !Files.exists(file)) {
            throw new ResourceNotFoundException("Menu image not found");
        }
        return new FileSystemResource(file);
    }

    public MediaType getMenuImageMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.IMAGE_JPEG;
    }

    private Path getMenuDirectory() {
        return Paths.get(uploadDir, "menus").toAbsolutePath().normalize();
    }

    private String resolveImageExtension(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (ALLOWED_IMAGE_TYPES.contains(contentType) || contentType.equals("image/jpg") || contentType.equals("image/pjpeg")) {
            return switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (original.endsWith(".png")) {
            return ".png";
        }
        if (original.endsWith(".webp")) {
            return ".webp";
        }
        if (original.endsWith(".gif")) {
            return ".gif";
        }
        if (original.endsWith(".jpg") || original.endsWith(".jpeg")) {
            return ".jpg";
        }
        if (contentType.startsWith("image/")) {
            return ".jpg";
        }
        return null;
    }

}
