package com.hangout.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final int BATCH_SIZE = 50;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public EmbeddingClient(@Value("${ai.api-key:}") String apiKey,
                           @Value("${ai.embedding-model:gemini-embedding-001}") String model,
                           @Value("${ai.timeout-seconds:20}") int timeoutSeconds) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        log.info("EmbeddingClient initialisé : modèle=[{}]", this.model);
    }

    /** Embedding d'une demande utilisateur. */
    public double[] embedQuery(String text) {
        return embed(List.of(text), "RETRIEVAL_QUERY").get(0);
    }

    /** Embeddings de documents (avis), par lots. */
    public List<double[]> embedDocuments(List<String> texts) {
        List<double[]> all = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            all.addAll(embed(texts.subList(i, Math.min(i + BATCH_SIZE, texts.size())),
                    "RETRIEVAL_DOCUMENT"));
        }
        return all;
    }

    private List<double[]> embed(List<String> texts, String taskType) {
        if (apiKey.isBlank()) {
            throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service IA non configuré (clé API absente)");
        }

        List<Map<String, Object>> requests = new ArrayList<>();
        for (String t : texts) {
            requests.add(Map.of(
                    "model", "models/" + model,
                    "content", Map.of("parts", List.of(Map.of("text", t))),
                    "taskType", taskType));
        }

        try {
            JsonNode res = restClient.post()
                    .uri("/v1beta/models/{model}:batchEmbedContents", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("requests", requests))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode embeddings = res == null ? null : res.path("embeddings");
            if (embeddings == null || !embeddings.isArray() || embeddings.size() != texts.size()) {
                log.warn("Réponse d'embeddings inattendue : {}", res);
                throw new AiException(HttpStatus.BAD_GATEWAY, "Réponse d'embeddings invalide");
            }

            List<double[]> out = new ArrayList<>();
            for (JsonNode e : embeddings) {
                JsonNode values = e.path("values");
                double[] v = new double[values.size()];
                for (int i = 0; i < v.length; i++) v[i] = values.get(i).asDouble();
                if (v.length == 0) {
                    throw new AiException(HttpStatus.BAD_GATEWAY, "Embedding vide");
                }
                out.add(v);
            }
            return out;

        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            log.warn("Erreur embeddings {} pour le modèle [{}] : {}",
                    code, model, e.getResponseBodyAsString());
            if (code == 429) {
                throw new AiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Quota IA dépassé, réessayez dans une minute");
            }
            if (code == 401 || code == 403) {
                throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Clé API IA invalide ou non autorisée");
            }
            if (code == 404) {
                throw new AiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Modèle d'embeddings introuvable, vérifiez LLM_EMBEDDING_MODEL");
            }
            throw new AiException(HttpStatus.BAD_GATEWAY, "Erreur du service d'embeddings (code " + code + ")");

        } catch (ResourceAccessException e) {
            throw new AiException(HttpStatus.GATEWAY_TIMEOUT,
                    "Le service IA ne répond pas (timeout)");
        } catch (RestClientException e) {
            throw new AiException(HttpStatus.BAD_GATEWAY, "Erreur de communication avec le service IA");
        }
    }
}