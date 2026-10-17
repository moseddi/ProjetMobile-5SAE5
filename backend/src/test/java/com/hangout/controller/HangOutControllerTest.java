package com.hangout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hangout.dto.HangOutResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.GlobalExceptionHandler;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.service.HangOutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer (controller) tests for {@link HangOutController}: HTTP status codes,
 * request validation and the JSON contract, with the service mocked.
 */
@DisplayName("HangOutController - web layer")
class HangOutControllerTest {

    private HangOutService hangOutService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        hangOutService = mock(HangOutService.class);

        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter();
        jackson.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));

        mockMvc = MockMvcBuilders.standaloneSetup(new HangOutController(hangOutService))
            .setMessageConverters(jackson)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator())
            .build();
    }

    @Test
    @DisplayName("GET /api/hangouts returns the list")
    void listReturnsHangouts() throws Exception {
        when(hangOutService.getAllHangOuts(false)).thenReturn(List.of(hangout("h1", "Friday Dinner")));

        mockMvc.perform(get("/api/hangouts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("h1"))
            .andExpect(jsonPath("$[0].title").value("Friday Dinner"))
            .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/hangouts?upcoming=true forwards the flag to the service")
    void listUpcomingForwardsFlag() throws Exception {
        when(hangOutService.getAllHangOuts(true)).thenReturn(List.of());

        mockMvc.perform(get("/api/hangouts").param("upcoming", "true"))
            .andExpect(status().isOk());

        verify(hangOutService).getAllHangOuts(true);
    }

    @Test
    @DisplayName("POST /api/hangouts creates a hangout and returns 200")
    void createReturnsCreatedHangout() throws Exception {
        when(hangOutService.createHangOut(eq("u1"), any())).thenReturn(hangout("h2", "New Dinner"));

        mockMvc.perform(post("/api/hangouts")
                .param("userId", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantId\":\"r1\",\"title\":\"New Dinner\",\"description\":\"d\","
                    + "\"plannedDateTime\":\"2999-01-01T10:00:00\",\"maxParticipants\":4,\"aiGenerated\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("h2"))
            .andExpect(jsonPath("$.title").value("New Dinner"));
    }

    @Test
    @DisplayName("POST /api/hangouts with an empty body is rejected with 400 field errors")
    void createWithMissingFieldsIsRejected() throws Exception {
        mockMvc.perform(post("/api/hangouts")
                .param("userId", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.restaurantId").exists())
            .andExpect(jsonPath("$.errors.title").exists())
            .andExpect(jsonPath("$.errors.plannedDateTime").exists())
            .andExpect(jsonPath("$.errors.maxParticipants").exists());
    }

    @Test
    @DisplayName("POST /api/hangouts with a past date is rejected with 400")
    void createWithPastDateIsRejected() throws Exception {
        mockMvc.perform(post("/api/hangouts")
                .param("userId", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantId\":\"r1\",\"title\":\"Past\","
                    + "\"plannedDateTime\":\"2000-01-01T10:00:00\",\"maxParticipants\":4}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.plannedDateTime").exists());
    }

    @Test
    @DisplayName("GET an unknown hangout returns 404")
    void getUnknownHangoutReturns404() throws Exception {
        when(hangOutService.getHangOutById("missing"))
            .thenThrow(new ResourceNotFoundException("Hangout not found"));

        mockMvc.perform(get("/api/hangouts/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Hangout not found"));
    }

    @Test
    @DisplayName("POST join returns 200")
    void joinReturnsHangout() throws Exception {
        when(hangOutService.joinHangOut("h1", "u2")).thenReturn(hangout("h1", "Friday Dinner"));

        mockMvc.perform(post("/api/hangouts/h1/join").param("userId", "u2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("h1"));
    }

    @Test
    @DisplayName("DELETE a hangout by its organizer returns 200")
    void cancelByOrganizerReturns200() throws Exception {
        mockMvc.perform(delete("/api/hangouts/h1").param("userId", "u1"))
            .andExpect(status().isOk());

        verify(hangOutService).cancelHangOut("h1", "u1");
    }

    @Test
    @DisplayName("DELETE by a non-organizer returns 400 with the business message")
    void cancelByNonOrganizerReturns400() throws Exception {
        doThrow(new BadRequestException("Only the organizer can cancel this hangout"))
            .when(hangOutService).cancelHangOut("h1", "u2");

        mockMvc.perform(delete("/api/hangouts/h1").param("userId", "u2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only the organizer can cancel this hangout"));
    }

    private HangOutResponse hangout(String id, String title) {
        HangOutResponse response = new HangOutResponse();
        response.setId(id);
        response.setTitle(title);
        response.setCreatorId("u1");
        response.setCreatorName("Demo");
        response.setRestaurantId("r1");
        response.setRestaurantName("Bella Italia");
        response.setPlannedDateTime(LocalDateTime.of(2999, 1, 1, 10, 0));
        response.setMaxParticipants(4);
        response.setStatus("OPEN");
        response.setAiGenerated(false);
        response.setParticipants(List.of());
        response.setJoinedCount(1);
        return response;
    }

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
