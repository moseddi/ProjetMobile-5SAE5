package com.hangout.controller;

import com.hangout.config.JwtAuthFilter;
import com.hangout.config.SecurityConfig;
import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.AvailabilityResult.Reason;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ConflictException;
import com.hangout.exception.ForbiddenException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.User;
import com.hangout.service.JwtService;
import com.hangout.service.AvailabilityService;
import com.hangout.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
class ReservationControllerTest {
    
    private static final String FUTURE = LocalDateTime.now().plusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0).toString();
    private static final String VALID_BODY = """
        {"restaurantId":"r1","reservationDateTime":"%s","partySize":2}""".formatted(FUTURE);
    
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @MockBean ReservationService reservationService;
    @MockBean AvailabilityService availabilityService;
    
    private String bearer(String userId) {
        User user = new User();
        user.setId(userId);
        return "Bearer " + jwtService.generateToken(user);
    }
    
    @Test
    void createReturns200WithSavedReservation() throws Exception {
        Reservation saved = new Reservation();
        saved.setId("new-id");
        when(reservationService.createReservation(eq("u1"), any())).thenReturn(saved);
        
        mvc.perform(post("/api/reservations").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("new-id"));
    }
    
    @Test
    void createWithInvalidPartySizeReturns400WithFieldError() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace("\"partySize\":2", "\"partySize\":0")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.partySize").exists());
        verifyNoInteractions(reservationService);
    }
    
    @Test
    void createWithPastDateReturns400() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY.replace(FUTURE, "2020-01-01T20:00:00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.reservationDateTime").exists());
    }
    
    @Test
    void createOnFullSlotReturns409() throws Exception {
        when(reservationService.createReservation(eq("u1"), any()))
            .thenThrow(new ConflictException("Ce créneau n'a plus assez de places disponibles"));
        
        mvc.perform(post("/api/reservations").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Ce créneau n'a plus assez de places disponibles"));
    }
    
    @Test
    void createWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(reservationService);
    }
    
    @Test
    void createWithForgedTokenReturns401() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1MSJ9.fake")
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(reservationService);
    }
    
    @Test
    void userIdParameterIsIgnoredInFavourOfToken() throws Exception {
        Reservation saved = new Reservation();
        when(reservationService.createReservation(eq("u1"), any())).thenReturn(saved);
        mvc.perform(post("/api/reservations").param("userId", "victim").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isOk());
        org.mockito.Mockito.verify(reservationService).createReservation(eq("u1"), any());
    }
    
    @Test
    void cancelWithoutTokenReturns401() throws Exception {
        mvc.perform(delete("/api/reservations/res1"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/reservations").header("Authorization", bearer("u1"))
                .contentType(MediaType.APPLICATION_JSON).content("{not json"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void updateByOtherUserReturns403() throws Exception {
        when(reservationService.updateReservation(eq("res1"), eq("intruder"), any(), any(), any(), any()))
            .thenThrow(new ForbiddenException("Cette réservation ne vous appartient pas"));
        
        mvc.perform(put("/api/reservations/res1").header("Authorization", bearer("intruder"))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void cancelReturns200() throws Exception {
        mvc.perform(delete("/api/reservations/res1").header("Authorization", bearer("u1")))
            .andExpect(status().isOk());
    }
    
    @Test
    void cancelUnknownReservationReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Reservation not found"))
            .when(reservationService).cancelReservation("nope", "u1");
        mvc.perform(delete("/api/reservations/nope").header("Authorization", bearer("u1")))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void cancelAlreadyCancelledReturns400() throws Exception {
        doThrow(new BadRequestException("Une réservation CANCELLED ne peut plus être modifiée"))
            .when(reservationService).cancelReservation("res1", "u1");
        mvc.perform(delete("/api/reservations/res1").header("Authorization", bearer("u1")))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void availabilityReturnsComputedResult() throws Exception {
        when(availabilityService.checkAvailability(eq("r1"), any(), anyInt()))
            .thenReturn(new AvailabilityResult(false, "r1", LocalDateTime.parse(FUTURE), 4, 1,
                Reason.NOT_ENOUGH_SEATS, List.of(LocalDateTime.parse(FUTURE).plusHours(1))));
        
        mvc.perform(get("/api/reservations/availability")
                .param("restaurantId", "r1").param("dateTime", FUTURE).param("partySize", "4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.reason").value("NOT_ENOUGH_SEATS"))
            .andExpect(jsonPath("$.alternatives.length()").value(1));
    }
    
    @Test
    void availabilityWithInvalidDateReturns400() throws Exception {
        mvc.perform(get("/api/reservations/availability")
                .param("restaurantId", "r1").param("dateTime", "demain").param("partySize", "4"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(reservationService.getReservationForUser("boom", "u1"))
            .thenThrow(new IllegalStateException("mongodb+srv://user:secret@cluster"));
        mvc.perform(get("/api/reservations/boom").header("Authorization", bearer("u1")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("secret"))));
    }
}
