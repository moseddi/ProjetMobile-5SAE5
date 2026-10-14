package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "review_chunks")
public class ReviewChunk {
    @Id
    private String id;

    private String reviewId;
    private String restaurantId;
    private Double rating;
    private String text;          // texte nettoyé de l'avis (affiché et envoyé au LLM)
    private String textHash;      // détecte les avis modifiés
    private double[] embedding;   // vecteur sémantique
    private LocalDateTime indexedAt;
}