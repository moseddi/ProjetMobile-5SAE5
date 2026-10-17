package com.hangout.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.ai.client.ClaudeClient;
import com.hangout.ai.model.DishChunk;
import com.hangout.ai.service.AssistantService;
import com.hangout.ai.service.RagService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistant;
    private final RagService rag;
    private final ClaudeClient llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public AssistantController(AssistantService assistant, RagService rag, ClaudeClient llm) {
        this.assistant = assistant;
        this.rag = rag;
        this.llm = llm;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        return Map.of("answer", assistant.chat(body.get("message")));
    }

    @PostMapping("/photo")
    public Map<String, Object> photo(@RequestParam("image") MultipartFile image) throws Exception {
        if (image.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Image trop lourde (5 Mo max)");

        String description = llm.askWithImage(
            "Décris uniquement le plat visible (nom probable, ingrédients, type de cuisine) en une phrase.",
            image.getBytes(), image.getContentType());

        List<DishChunk> hits = rag.retrieve(description, 5);

        String answer = llm.ask(
            "Tu es l'assistant d'une app de restauration. Utilise UNIQUEMENT les plats fournis. "
          + "Dis « ça ressemble à... » sans certitude. Si la liste est vide, dis qu'aucun restaurant ne le propose.",
            "Plat photographié : " + description + "\nPlats trouvés : " + mapper.writeValueAsString(hits));

        return Map.of("description", description, "answer", answer, "matches", hits);
    }
}
