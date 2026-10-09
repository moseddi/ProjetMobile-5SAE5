package com.hangout.service;

import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    public Event getById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement introuvable : " + id));
    }

    public List<Event> getByRestaurant(String restaurantId) {
        return eventRepository.findByRestaurantId(restaurantId);
    }

    public Event create(Event event) {
        event.setId(null);
        if (event.getBookedSpots() == null) event.setBookedSpots(0);
        return eventRepository.save(event);
    }

    public Event update(String id, Event data) {
        Event existing = getById(id);
        data.setId(existing.getId());
        data.setBookedSpots(existing.getBookedSpots());
        return eventRepository.save(data);
    }

    public void delete(String id) {
        getById(id);
        eventRepository.deleteById(id);
    }
}