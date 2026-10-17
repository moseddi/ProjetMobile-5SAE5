package com.hangout.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class EmbeddingService {
    private final RestClient http;
    private final String model;

    public EmbeddingService(@Value("${llm.base-url}") String baseUrl,
                            @Value("${llm.api-key}") String key,
                            @Value("${llm.embedding-model}") String model) {
        this.model = model;
        this.http = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + key)
            .build();
    }

    public List<Double> embed(String text) {
        JsonNode r = http.post().uri("/embeddings")
            .body(Map.of("model", model, "input", text))
            .retrieve().body(JsonNode.class);
        List<Double> v = new ArrayList<>();
        r.get("data").get(0).get("embedding").forEach(x -> v.add(x.asDouble()));
        return v;
    }
}
