package com.hangout.service;

import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Reservation;
import com.hangout.model.User;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    
    public List<Reservation> getUserReservations(String userId) {
        return reservationRepository.findByUserId(userId);
    }
    
    public Reservation getReservationById(String id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
    
    @Transactional
    public Reservation createReservation(String userId, Reservation reservation) {
        reservation.setUserId(userId);
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        
        Reservation saved = reservationRepository.save(reservation);
        
        // Update user stats
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setTotalReservations(user.getTotalReservations() + 1);
        userRepository.save(user);
        
        return saved;
    }
    
    @Transactional
    public void cancelReservation(String id) {
        Reservation reservation = getReservationById(id);
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
}
