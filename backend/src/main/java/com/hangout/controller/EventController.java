package com.hangout.controller;

import com.hangout.model.Event;
import com.hangout.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.hangout.ai.EventDescriptionService;
import com.hangout.ai.GenerateDescriptionRequest;
import com.hangout.ai.GeneratedDescription;
import com.hangout.ai.SimpleRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventDescriptionService descriptionService;
    private final SimpleRateLimiter rateLimiter;

    @GetMapping
    public List<Event> getAll() { return eventService.getAll(); }

    @GetMapping("/{id}")
    public Event getById(@PathVariable String id) { return eventService.getById(id); }

    @GetMapping("/restaurant/{restaurantId}")
    public List<Event> getByRestaurant(@PathVariable String restaurantId) {
        return eventService.getByRestaurant(restaurantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Event create(@RequestBody Event event) { return eventService.create(event); }

    @PutMapping("/{id}")
    public Event update(@PathVariable String id, @RequestBody Event event) {
        return eventService.update(id, event);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { eventService.delete(id); }

    @PostMapping("/generate-description")
public GeneratedDescription generateDescription(
        @Valid @RequestBody GenerateDescriptionRequest request,
        HttpServletRequest http) {
    rateLimiter.check(http.getRemoteAddr());
    return descriptionService.generate(request);
}
}
