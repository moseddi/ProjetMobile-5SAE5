package com.hangout.repository;

import com.hangout.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByRestaurantId(String restaurantId);
    List<Event> findByCategory(Event.EventCategory category);
}
