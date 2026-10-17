package com.hangout.chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.chatbot.AiServiceException.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls Groq's OpenAI-compatible chat completions endpoint and returns the parsed extraction.
 * The API key is only ever placed in the Authorization header; it is never logged or returned.
 */
@Slf4j
@Component
public class GroqClient {
    
    private final RestClient restClient;
    private final GroqProperties properties;
    private final PromptFactory promptFactory;
    private final ObjectMapper objectMapper;
    
    public GroqClient(@Qualifier("groqRestClient") RestClient restClient, GroqProperties properties,
                      PromptFactory promptFactory, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }
    
    /**
     * @param messages system + conversation messages, each {"role": ..., "content": ...}
     */
    public ExtractedRequest extract(List<Map<String, String>> messages) {
        if (!properties.hasApiKey()) {
            throw new AiServiceException(Type.NOT_CONFIGURED, "La variable d'environnement GROQ_API_KEY n'est pas définie");
        }
        JsonNode body = post(buildRequest(messages));
        return parse(body);
    }
    
    Map<String, Object> buildRequest(List<Map<String, String>> messages) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel());
        request.put("messages", messages);
        request.put("temperature", 0);
        request.put("max_completion_tokens", properties.getMaxCompletionTokens());
        if (properties.isStructuredOutput()) {
            request.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                    "name", "reservation_request",
                    "strict", true,
                    "schema", promptFactory.responseSchema())));
        } else {
            request.put("response_format", Map.of("type", "json_object"));
        }
        if (properties.getModel().startsWith("openai/gpt-oss")) {
            // Short internal reasoning is enough for extraction and keeps latency and token usage low
            request.put("reasoning_effort", "low");
            request.put("include_reasoning", false);
        }
        return request;
    }
    
    private JsonNode post(Map<String, Object> request) {
        try {
            return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            log.warn("Groq a refusé la requête : HTTP {}", e.getStatusCode().value());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                throw new AiServiceException(Type.AUTHENTICATION, "Clé API Groq refusée", e);
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                throw new AiServiceException(Type.RATE_LIMITED, "Quota Groq atteint", e);
            }
            throw new AiServiceException(Type.INVALID_REQUEST, "Requête refusée par Groq", e);
        } catch (HttpServerErrorException e) {
            log.warn("Groq indisponible : HTTP {}", e.getStatusCode().value());
            throw new AiServiceException(Type.UNAVAILABLE, "Service Groq indisponible", e);
        } catch (ResourceAccessException e) {
            log.warn("Groq injoignable ou délai dépassé : {}", e.getClass().getSimpleName());
            throw new AiServiceException(Type.UNAVAILABLE, "Service Groq injoignable", e);
        } catch (RestClientException e) {
            throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse illisible de Groq", e);
        }
    }
    
    private ExtractedRequest parse(JsonNode body) {
        JsonNode choice = body == null ? null : body.path("choices").path(0);
        if (choice == null || choice.isMissingNode()) {
            throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse Groq sans résultat");
        }
        if ("length".equals(choice.path("finish_reason").asText())) {
            throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse Groq tronquée");
        }
        String content = choice.path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse Groq vide");
        }
        try {
            ExtractedRequest extracted = objectMapper.readValue(stripCodeFence(content), ExtractedRequest.class);
            if (extracted == null) {
                throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse Groq vide");
            }
            return extracted;
        } catch (JsonProcessingException e) {
            throw new AiServiceException(Type.INVALID_RESPONSE, "Réponse Groq qui n'est pas du JSON valide", e);
        }
    }
    
    // Some models wrap JSON in ```json fences when not in strict mode
    private static String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }
}
