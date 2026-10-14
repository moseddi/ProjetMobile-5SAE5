package com.hangout.service;

import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==========================================================================
 * 🎟️ MODULE : ÉVÉNEMENTS (TRAVAIL DE MA COLLÈGUE / FRIEND'S MODULE)
 * Auteur : Binôme Événements
 * Rôle : Gestion des événements, réservations et intégration IA
 * ==========================================================================
 * Tests Unitaires automatisés (Mockito) pour EventService.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event();
        sampleEvent.setId("evt-101");
        sampleEvent.setRestaurantId("resto-1");
        sampleEvent.setTitle("Soirée Dégustation Vins & Fromages");
        sampleEvent.setDescription("Découvrez notre sélection exclusive de grands crus.");
        sampleEvent.setEventDate(LocalDateTime.now().plusDays(5));
        sampleEvent.setPrice(45.0);
        sampleEvent.setAvailableSpots(30);
        sampleEvent.setBookedSpots(0);
        sampleEvent.setCategory(Event.EventCategory.WINE_TASTING);
    }

    @Test
    @DisplayName("GIVEN existing events WHEN getAll() THEN return full list")
    void shouldReturnAllEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(sampleEvent));

        List<Event> result = eventService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Soirée Dégustation Vins & Fromages");
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("GIVEN valid ID WHEN getById() THEN return correct Event")
    void shouldReturnEventById() {
        when(eventRepository.findById("evt-101")).thenReturn(Optional.of(sampleEvent));

        Event result = eventService.getById("evt-101");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("evt-101");
        verify(eventRepository, times(1)).findById("evt-101");
    }

    @Test
    @DisplayName("GIVEN unknown ID WHEN getById() THEN throw ResourceNotFoundException")
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById("unknown-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Événement introuvable");
    }

    @Test
    @DisplayName("GIVEN valid Event WHEN create() THEN reset ID, initialize booked spots, and save")
    void shouldCreateEventSuccessfully() {
        Event toCreate = new Event();
        toCreate.setTitle("Cooking Masterclass");
        toCreate.setRestaurantId("resto-1");
        toCreate.setBookedSpots(null); // Should be initialized to 0

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId("evt-gen-1");
            return saved;
        });

        Event created = eventService.create(toCreate);

        assertThat(created.getId()).isEqualTo("evt-gen-1");
        assertThat(created.getBookedSpots()).isEqualTo(0);
        verify(eventRepository, times(1)).save(toCreate);
    }

    @Test
    @DisplayName("GIVEN restaurantId WHEN getByRestaurant() THEN return filtered events")
    void shouldReturnEventsByRestaurant() {
        when(eventRepository.findByRestaurantId("resto-1")).thenReturn(List.of(sampleEvent));

        List<Event> result = eventService.getByRestaurant("resto-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestaurantId()).isEqualTo("resto-1");
        verify(eventRepository, times(1)).findByRestaurantId("resto-1");
    }

    @Test
    @DisplayName("GIVEN valid ID WHEN delete() THEN verify existence and delete")
    void shouldDeleteEventSuccessfully() {
        when(eventRepository.findById("evt-101")).thenReturn(Optional.of(sampleEvent));
        doNothing().when(eventRepository).deleteById("evt-101");

        eventService.delete("evt-101");

        verify(eventRepository, times(1)).findById("evt-101");
        verify(eventRepository, times(1)).deleteById("evt-101");
    }
}