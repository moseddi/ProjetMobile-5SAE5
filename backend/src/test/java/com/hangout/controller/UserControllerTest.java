package com.hangout.controller;

import com.hangout.config.JwtAuthFilter;
import com.hangout.config.SecurityConfig;
import com.hangout.model.User;
import com.hangout.repository.EventBookingRepository;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.UserRepository;
import com.hangout.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
class UserControllerTest {
    
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @MockBean UserRepository userRepository;
    @MockBean ReservationRepository reservationRepository;
    @MockBean EventBookingRepository eventBookingRepository;
    
    private String bearer(String userId) {
        User user = new User();
        user.setId(userId);
        return "Bearer " + jwtService.generateToken(user);
    }
    
    @Test
    void userCanListOwnReservations() throws Exception {
        when(reservationRepository.findByUserId("u1")).thenReturn(List.of());
        mvc.perform(get("/api/users/u1/reservations").header("Authorization", bearer("u1")))
            .andExpect(status().isOk());
    }
    
    @Test
    void userCannotListSomeoneElsesReservations() throws Exception {
        mvc.perform(get("/api/users/u2/reservations").header("Authorization", bearer("u1")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(reservationRepository);
    }
    
    @Test
    void listingReservationsRequiresToken() throws Exception {
        mvc.perform(get("/api/users/u1/reservations"))
            .andExpect(status().isUnauthorized());
    }
}
