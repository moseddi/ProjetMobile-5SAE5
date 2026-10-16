package com.hangout.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hangout.ai.client.ClaudeClient;
import com.hangout.ai.model.DishChunk;
import com.hangout.ai.repository.DishChunkRepository;
import com.hangout.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssistantService {
    private final ClaudeClient llm;
    private final RagService rag;
    private final RestaurantRepository restaurants;
    private final DishChunkRepository chunkRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM =
        "Tu es l'assistant d'une app de restauration. Utilise les outils pour chercher. "
      + "Ne cite jamais un restaurant ou un plat absent des résultats des outils. "
      + "Si les outils ne renvoient rien, dis qu'aucun restaurant ne le propose. "
      + "Cite tous les plats pertinents trouvés, avec leur prix et leur restaurant. "
      + "Si le même plat existe dans plusieurs restaurants, cite-les tous, pas un seul. "
      + "Pour une catégorie (desserts, entrées, burgers), utilise search_dishes "
      + "puis get_menu avec category (dessert, starter...) SANS restaurantName, "
      + "pour obtenir tous les restaurants. N'appelle pas get_menu sur un seul restaurant "
      + "si la question porte sur une catégorie. "
      + "Pour une catégorie large (poisson, viande), fais plusieurs recherches search_dishes "
      + "avec des synonymes (fish, salmon, cod, seafood, dessert). "
      + "Les menus sont en anglais et en espagnol : formule toujours query en anglais "
      + "(fish, salmon, cod, chicken, dessert). "
      + "N'utilise maxPrice que si le client donne un montant précis. "
      + "Réponds toujours dans la langue exacte du dernier message du client "
      + "(anglais s'il écrit en anglais, français s'il écrit en français, arabe s'il écrit en arabe). "
      + "N'ajoute aucun symbole de devise : écris seulement le nombre (par exemple 8.00). "
      + "Sois concis.";

    private static Map<String, Object> tool(String name, String desc,
            Map<String, Object> props, List<String> required) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        if (!required.isEmpty()) params.put("required", required);
        return Map.of("type", "function",
            "function", Map.of("name", name, "description", desc, "parameters", params));
    }

    private static final List<Map<String, Object>> TOOLS = List.of(
        tool("search_restaurants", "Cherche des restaurants selon des filtres",
            Map.of("cuisine", Map.of("type", "string"),
                   "priceRange", Map.of("type", "string",
                       "enum", List.of("BUDGET", "MODERATE", "EXPENSIVE", "LUXURY")),
                   "openNow", Map.of("type", "boolean")),
            List.of()),
        tool("search_dishes", "Cherche des plats par description en anglais (ex: fish, pasta, dessert). "
            + "Utilise maxPrice uniquement si le client donne un budget chiffré. "
            + "Si maxPrice est donné, les résultats sont triés du moins cher au plus cher.",
            Map.of("query", Map.of("type", "string"),
                   "maxPrice", Map.of("type", "number")),
            List.of("query")),
        tool("get_menu", "Liste des plats. Sans restaurantName : tous les restaurants. "
            + "Avec category (dessert, starter, burger...) : filtre cette catégorie partout.",
            Map.of("restaurantName", Map.of("type", "string"),
                   "category", Map.of("type", "string")),
            List.of())
    );

    public AssistantService(ClaudeClient llm, RagService rag,
                            RestaurantRepository restaurants, DishChunkRepository chunkRepo) {
        this.llm = llm;
        this.rag = rag;
        this.restaurants = restaurants;
        this.chunkRepo = chunkRepo;
    }

    public String chat(String userMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM));
        messages.add(Map.of("role", "user", "content", userMessage));

        for (int i = 0; i < 5; i++) {
            JsonNode msg = llm.call(messages, TOOLS);

            if (!msg.hasNonNull("tool_calls") || msg.get("tool_calls").isEmpty()) {
                return msg.hasNonNull("content") ? msg.get("content").asText() : "";
            }

            messages.add(mapper.convertValue(msg, new TypeReference<Map<String, Object>>() {}));

            for (JsonNode call : msg.get("tool_calls")) {
                String out;
                try {
                    JsonNode args = mapper.readTree(call.get("function").get("arguments").asText());
                    out = executeTool(call.get("function").get("name").asText(), args);
                } catch (Exception e) {
                    out = "{\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
                }
                messages.add(Map.of("role", "tool",
                    "tool_call_id", call.get("id").asText(), "content", out));
            }
        }
        return "Désolé, je n'ai pas pu terminer la demande.";
    }

    private String executeTool(String name, JsonNode in) throws Exception {
        return switch (name) {
            case "search_restaurants" -> mapper.writeValueAsString(searchRestaurants(in));
            case "search_dishes" -> {
                String q = in.get("query").asText();
                Double max = in.hasNonNull("maxPrice") ? in.get("maxPrice").asDouble() : null;
                List<DishChunk> res = rag.retrieve(q, 12, max);
                if (res.isEmpty() && max != null) res = rag.retrieve(q, 12, null);
                yield mapper.writeValueAsString(res);
            }
            case "get_menu" -> {
                String n = in.hasNonNull("restaurantName") ? in.get("restaurantName").asText().toLowerCase() : "";
                String cat = in.hasNonNull("category") ? in.get("category").asText().toLowerCase() : "";
                yield mapper.writeValueAsString(chunkRepo.findAll().stream()
                    .filter(c -> n.isBlank() || (c.getRestaurantName() != null
                        && c.getRestaurantName().toLowerCase().contains(n)))
                    .filter(c -> cat.isBlank() || matchesCategory(c, cat))
                    .toList());
            }
            default -> "{\"error\":\"outil inconnu\"}";
        };
    }

    private static boolean matchesCategory(DishChunk chunk, String category) {
        String blob = ((chunk.getDishName() == null ? "" : chunk.getDishName()) + " "
            + (chunk.getText() == null ? "" : chunk.getText())).toLowerCase();
        if (blob.contains(category)) {
            return true;
        }
        if (category.contains("dessert") || category.contains("sweet")) {
            return blob.contains("dessert") || blob.contains("cake") || blob.contains("ice cream")
                || blob.contains("tiramisu") || blob.contains("pastry") || blob.contains("chocolate");
        }
        return false;
    }

    private List<Map<String, Object>> searchRestaurants(JsonNode in) {
        String cuisine = in.hasNonNull("cuisine") ? in.get("cuisine").asText().toLowerCase() : null;
        String price = in.hasNonNull("priceRange") ? in.get("priceRange").asText() : null;
        Boolean openNow = in.hasNonNull("openNow") ? in.get("openNow").asBoolean() : null;

        return restaurants.findAll().stream()
            .filter(r -> cuisine == null || (r.getCuisine() != null
                && r.getCuisine().toLowerCase().contains(cuisine)))
            .filter(r -> price == null || (r.getPriceRange() != null
                && r.getPriceRange().toString().equals(price)))
            .filter(r -> openNow == null || Boolean.TRUE.equals(r.getIsOpen()) == openNow)
            .limit(10)
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("name", r.getName());
                m.put("cuisine", r.getCuisine());
                m.put("priceRange", String.valueOf(r.getPriceRange()));
                m.put("address", r.getAddress());
                m.put("isOpen", r.getIsOpen());
                return m;
            }).toList();
    }
}