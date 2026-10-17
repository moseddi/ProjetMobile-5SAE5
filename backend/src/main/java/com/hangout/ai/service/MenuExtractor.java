package com.hangout.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.ai.client.ClaudeClient;
import com.hangout.ai.model.Dish;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MenuExtractor {
    private final ClaudeClient claude;
    private final ObjectMapper mapper = new ObjectMapper();

    public MenuExtractor(ClaudeClient claude) { this.claude = claude; }

    public List<Dish> extract(byte[] image, String mediaType) throws Exception {
        String json = claude.askWithImage("""
            Lis ce menu de restaurant. Réponds UNIQUEMENT avec un tableau JSON, sans texte autour :
            [{"name":"","description":"","category":"","vegetarian":false,"price":0}]
            Règles : n'invente rien, ignore le texte factice (lorem ipsum), n'inclus pas de doublons,
            mets null si une information est absente. Si l'image n'est pas un menu, réponds [].""",
            image, mediaType);

        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end < start) return List.of();

        List<Dish> all = mapper.readValue(json.substring(start, end + 1),
                new TypeReference<List<Dish>>() {});

        Set<String> seen = new HashSet<>();
        return all.stream()
            .filter(d -> d.name() != null && !d.name().isBlank())
            .filter(d -> !d.name().toLowerCase().contains("lorem ipsum"))
            .filter(d -> seen.add(d.name().toLowerCase()))
            .toList();
    }
}