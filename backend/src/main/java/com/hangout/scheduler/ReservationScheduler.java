package com.hangout.scheduler;

import com.hangout.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels accepted reservations whose guest is more than 30 minutes late and tells the guest.
 */
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    @Scheduled(initialDelay = 15_000, fixedDelay = 60_000)
    public void cancelLateReservations() {
        reservationService.cancelLateReservations();
    }
}
