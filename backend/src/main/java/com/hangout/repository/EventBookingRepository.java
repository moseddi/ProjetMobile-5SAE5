package com.hangout.repository;

import com.hangout.model.EventBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventBookingRepository extends MongoRepository<EventBooking, String> {
    List<EventBooking> findByUserId(String userId);
    List<EventBooking> findByEventId(String eventId);
}
