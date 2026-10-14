package com.hangout.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client LLM branché sur Groq (API compatible OpenAI).
 * Remplace Gemini pour la génération d'explications RAG.
 */
@Service
public class LlmClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public LlmClient(@Value("${ai.groq-api-key:}") String apiKey,
                     @Value("${ai.groq-model:openai/gpt-oss-120b}") String model,
                     @Value("${ai.timeout-seconds:30}") int timeoutSeconds) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.groq.com/openai/v1")
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Envoie un prompt système + un contenu utilisateur à Groq.
     * Retourne le texte brut généré.
     */
    public String complete(String systemPrompt, String userContent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service IA non configuré (clé API absente)");
        }

        // Format OpenAI chat/completions
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.7,
                "max_tokens", 2048,
                "response_format", Map.of("type", "json_object")
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = response == null ? "" :
                    response.path("choices").path(0)
                            .path("message").path("content")
                            .asText("");

            if (text.isBlank()) {
                throw new AiException(HttpStatus.BAD_GATEWAY, "Réponse vide du service IA");
            }
            return text;

        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            if (code == 429) {
                throw new AiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Quota IA dépassé, réessayez dans une minute");
            }
            if (code == 401 || code == 403) {
                throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Clé API IA invalide ou non autorisée");
            }
            if (code == 400) {
                throw new AiException(HttpStatus.BAD_GATEWAY,
                        "Requête IA refusée (clé ou modèle invalide)");
            }
            if (code == 404) {
                throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Modèle IA introuvable, vérifiez LLM_MODEL");
            }
            throw new AiException(HttpStatus.BAD_GATEWAY, "Erreur du service IA (code " + code + ")");

        } catch (ResourceAccessException e) {
            throw new AiException(HttpStatus.GATEWAY_TIMEOUT,
                    "Le service IA ne répond pas (timeout)");

        } catch (RestClientException e) {
            throw new AiException(HttpStatus.BAD_GATEWAY, "Erreur de communication avec le service IA");
        }
    }
}