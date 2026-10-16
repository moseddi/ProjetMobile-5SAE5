package com.hangout.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class ClaudeClient {
    private final RestClient http;
    private final String model;

    public ClaudeClient(@Value("${llm.base-url}") String baseUrl,
                        @Value("${llm.api-key}") String key,
                        @Value("${llm.model}") String model) {
        this.model = model;
        this.http = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + key)
            .build();
    }

    public JsonNode call(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    if (tools != null) body.put("tools", tools);

    for (int attempt = 1; ; attempt++) {
        try {
            JsonNode r = http.post().uri("/chat/completions").body(body)
                .retrieve().body(JsonNode.class);
            return r.get("choices").get(0).get("message");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if ((code == 503 || code == 429) && attempt < 4) {
                try { Thread.sleep(3000L * attempt); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
                continue;
            }
            throw e;
        }
    }
}

    public String ask(String system, String user) {
        List<Map<String, Object>> msgs = new ArrayList<>();
        if (system != null) msgs.add(Map.of("role", "system", "content", system));
        msgs.add(Map.of("role", "user", "content", user));
        return call(msgs, null).get("content").asText();
    }

    public String askWithImage(String instruction, byte[] image, String mediaType) {
        String dataUrl = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        List<Object> content = List.of(
            Map.of("type", "text", "text", instruction),
            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)));
        List<Map<String, Object>> msgs = List.of(Map.of("role", "user", "content", content));
        return call(msgs, null).get("content").asText();
    }
}
