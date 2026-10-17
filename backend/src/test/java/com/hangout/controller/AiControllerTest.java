package com.hangout.controller;

import com.hangout.dto.AiHangoutResponse;
import com.hangout.exception.GlobalExceptionHandler;
import com.hangout.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer (controller) tests for {@link AiController} using a standalone
 * MockMvc — no Spring context, no database. Focuses on the HTTP contract and
 * request validation.
 */
@DisplayName("AiController - web layer")
class AiControllerTest {

    private AiService aiService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiController(aiService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator())
            .build();
    }

    @Test
    @DisplayName("POST /api/ai/hangout-assistant returns 200 with the assistant response")
    void validRequestReturnsAssistantResponse() throws Exception {
        AiHangoutResponse response = new AiHangoutResponse();
        response.setAiGenerated(true);
        response.setProvider("gemini-test");
        response.setRestaurantId("r-it");
        response.setRestaurantName("Bella Italia");
        response.setTitle("Italian Night");
        when(aiService.suggestHangout(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/hangout-assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"italian dinner for friends\",\"partySize\":4}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiGenerated").value(true))
            .andExpect(jsonPath("$.provider").value("gemini-test"))
            .andExpect(jsonPath("$.restaurantName").value("Bella Italia"));
    }

    @Test
    @DisplayName("blank prompt is rejected with 400 and a field error")
    void blankPromptIsRejected() throws Exception {
        mockMvc.perform(post("/api/ai/hangout-assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.prompt").exists());
    }

    @Test
    @DisplayName("a party size below 2 is rejected with 400")
    void tooSmallPartySizeIsRejected() throws Exception {
        mockMvc.perform(post("/api/ai/hangout-assistant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"dinner\",\"partySize\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.partySize").exists());
    }

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
