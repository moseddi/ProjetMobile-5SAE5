package com.hangout.repository;

import com.hangout.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByRestaurantId(String restaurantId);
    List<Reservation> findByUserIdAndStatus(String userId, Reservation.ReservationStatus status);
    
    // Bounds are exclusive (Mongo $gt / $lt): returns reservations whose slot overlaps the requested one
    List<Reservation> findByRestaurantIdAndStatusInAndReservationDateTimeBetween(
        String restaurantId,
        Collection<Reservation.ReservationStatus> statuses,
        LocalDateTime from,
        LocalDateTime to);
}
