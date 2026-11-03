package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.dto.EventRequest;
import com.hangout.dto.EventResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.model.Restaurant;
import com.hangout.repository.EventRepository;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final RestaurantRepository restaurantRepository;

    public List<EventResponse> getAll() {
        return eventRepository.findAll().stream()
            // Defensive: an event can only be created with a restaurant today, but
            // guards against orphaned/legacy records that predate that guarantee.
            .filter(event -> event.getRestaurantId() != null)
            .sorted(Newest.first(Event::getId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<EventResponse> getByRestaurant(String restaurantId) {
        return eventRepository.findByRestaurantId(restaurantId).stream()
            .sorted(Newest.first(Event::getId))
            .map(this::toResponse).collect(Collectors.toList());
    }

    public EventResponse getById(String id) {
        return toResponse(getEventOrThrow(id));
    }

    public EventResponse create(String restaurantId, EventRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Event event = new Event();
        event.setRestaurantId(restaurant.getId());
        applyRequest(event, request);
        event.setBookedSpots(0);
        return toResponse(eventRepository.save(event));
    }

    public EventResponse update(String id, EventRequest request) {
        Event event = getEventOrThrow(id);
        applyRequest(event, request);
        return toResponse(eventRepository.save(event));
    }

    public void delete(String id) {
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found");
        }
        eventRepository.deleteById(id);
    }

    /**
     * Holds a spot for a registration attempt. The spot is held as soon as a client
     * registers (so the last spot can't be double-booked while the owner decides),
     * and released via {@link #releaseSpot} if the resulting reservation is cancelled.
     */
    @Transactional
    public Event reserveSpots(String eventId, int people) {
        Event event = getEventOrThrow(eventId);
        if (event.getEventDate() != null && event.getEventDate().isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestException("This event has already taken place");
        }
        int left = event.getAvailableSpots() - event.getBookedSpots();
        if (left <= 0) {
            throw new BadRequestException("This event is fully booked");
        }
        if (people > left) {
            throw new BadRequestException("Only " + left + (left == 1 ? " spot is" : " spots are") + " left for this event");
        }
        event.setBookedSpots(event.getBookedSpots() + people);
        return eventRepository.save(event);
    }

    @Transactional
    public void releaseSpots(String eventId, int people) {
        eventRepository.findById(eventId).ifPresent(event -> {
            event.setBookedSpots(Math.max(0, event.getBookedSpots() - people));
            eventRepository.save(event);
        });
    }

    public void assertOwnsEvent(String eventId, String ownerUserId) {
        Event event = getEventOrThrow(eventId);
        assertOwnsRestaurant(event.getRestaurantId(), ownerUserId);
    }

    public void assertOwnsRestaurant(String restaurantId, String ownerUserId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!ownerUserId.equals(restaurant.getOwnerId())) {
            throw new AccessDeniedException("You do not manage this restaurant");
        }
    }

    private void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setImage(request.getImage());
        event.setEventDate(request.getEventDate());
        event.setPrice(request.getPrice());
        event.setAvailableSpots(request.getAvailableSpots());
        event.setCategory(normalizeCategory(request.getCategory()));
    }

    private static final java.util.Set<String> BUILT_IN_CATEGORIES = java.util.Set.of(
        "WINE_TASTING", "COOKING_CLASS", "LIVE_MUSIC", "SPECIAL_DINNER", "THEMED_NIGHT", "OTHER"
    );

    /**
     * Owners may pick a built-in type or invent their own. Built-ins are stored under
     * their key; a custom type reuses the spelling of an identical one that already
     * exists, so "karaoke" and "Karaoke" don't split into two filters.
     */
    private String normalizeCategory(String raw) {
        String cleaned = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return "OTHER";
        }

        String asKey = cleaned.toUpperCase().replace(' ', '_');
        if (BUILT_IN_CATEGORIES.contains(asKey)) {
            return asKey;
        }

        return eventRepository.findAll().stream()
            .map(Event::getCategory)
            .filter(c -> c != null && !BUILT_IN_CATEGORIES.contains(c) && c.equalsIgnoreCase(cleaned))
            .findFirst()
            .orElse(cleaned);
    }

    private Event getEventOrThrow(String id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private EventResponse toResponse(Event event) {
        Restaurant restaurant = event.getRestaurantId() != null
            ? restaurantRepository.findById(event.getRestaurantId()).orElse(null)
            : null;

        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setRestaurantId(event.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setImage(event.getImage());
        response.setEventDate(event.getEventDate());
        response.setPrice(event.getPrice());
        response.setAvailableSpots(event.getAvailableSpots());
        response.setBookedSpots(event.getBookedSpots());
        response.setCategory(event.getCategory());
        return response;
    }
}
