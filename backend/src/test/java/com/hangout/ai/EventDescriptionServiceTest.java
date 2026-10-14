package com.hangout.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
/**
 * ==========================================================================
 * 🎟️ MODULE : ÉVÉNEMENTS & IA (TRAVAIL DE MA COLLÈGUE / FRIEND'S MODULE)
 * Auteur : Binôme Événements
 * Rôle : Générateur de Description Marketing par IA (Groq / LLM)
 * ==========================================================================
 * Tests Unitaires (Mockito) pour EventDescriptionService.
 */
@ExtendWith(MockitoExtension.class)
class EventDescriptionServiceTest {

    @Mock
    private LlmClient llmClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private EventDescriptionService descriptionService;

    private GenerateDescriptionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new GenerateDescriptionRequest(
                "Dégustation Truffes & Vins",
                "Samedi 20 Novembre",
                "Française Gastronomique",
                "Risotto aux truffes noires",
                "Chaleureuse et tamisée",
                25,
                "élégant et chaleureux"
        );
    }

    @Test
    @DisplayName("GIVEN valid LLM JSON output WHEN generate() THEN parse into GeneratedDescription DTO")
    void shouldParseValidLlmResponse() {
        String mockLlmJson = """
                {
                    "titre": "Magie de la Truffe Noire",
                    "description": "Une immersion sensorielle au coeur de la gastronomie française avec nos sommeliers.",
                    "hashtags": ["#Truffe", "#Gastronomie", "#HangOut", "#VinsDException"],
                    "appel_action": "Places limitées, réservez votre table dès aujourd'hui !"
                }
                """;

        when(llmClient.complete(anyString(), anyString())).thenReturn(mockLlmJson);

        GeneratedDescription result = descriptionService.generate(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.titre()).isEqualTo("Magie de la Truffe Noire");
        assertThat(result.hashtags()).contains("#Truffe", "#Gastronomie");
        assertThat(result.appelAction()).isNotEmpty();
    }

    @Test
    @DisplayName("GIVEN broken LLM output on first attempt and valid on retry WHEN generate() THEN succeed")
    void shouldRetryAndSucceedOnSecondAttempt() {
        String invalidJson = "Désolé, voici une description : Titre : Soirée";
        String validJson = """
                {
                    "titre": "Soirée Réussie",
                    "description": "Description valide après retry avec plus de cinquante caractères obligatoires.",
                    "hashtags": ["#Event", "#Food", "#Dinner", "#HangOut"],
                    "appel_action": "Réservez vite !"
                }
                """;

        when(llmClient.complete(anyString(), anyString()))
                .thenReturn(invalidJson)
                .thenReturn(validJson);

        GeneratedDescription result = descriptionService.generate(validRequest);

        assertThat(result.titre()).isEqualTo("Soirée Réussie");
    }

    @Test
    @DisplayName("GIVEN persistent LLM failure WHEN generate() THEN throw AiException")
    void shouldThrowAiExceptionWhenAllAttemptsFail() {
        when(llmClient.complete(anyString(), anyString())).thenReturn("Corrupted response...");

        assertThatThrownBy(() -> descriptionService.generate(validRequest))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("invalide");
    }
}