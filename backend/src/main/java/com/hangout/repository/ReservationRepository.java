package com.hangout.repository;

import com.hangout.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByRestaurantId(String restaurantId);
    List<Reservation> findByUserIdAndStatus(String userId, Reservation.ReservationStatus status);
    List<Reservation> findByUserIdAndEventId(String userId, String eventId);
    List<Reservation> findByStatus(Reservation.ReservationStatus status);
}
