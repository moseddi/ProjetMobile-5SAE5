package com.hangout.ai;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de recommandation par profil utilisateur.
 * Version minimale utilisée par le RAG pour le mode "PROFILE"
 * (quand le client ne saisit aucune demande).
 *
 * À enrichir plus tard avec tes vraies données (réservations, avis, favoris).
 */
@Service
public class RecommendationService {

    /** Résultat : liste des cuisines que l'utilisateur aime. */
    public record Result(List<String> likedCuisines) {}

    /**
     * Retourne les cuisines préférées de l'utilisateur.
     *
     * @param userId identifiant du client
     * @param limit  nombre maximum de cuisines à retourner
     * @return un Result contenant les cuisines préférées
     */
    public Result recommend(String userId, int limit) {
        // TODO : brancher sur tes vraies données.
        // Tant que la liste est vide, le RAG bascule automatiquement en mode DEFAULT.
        return new Result(List.of());
    }
}