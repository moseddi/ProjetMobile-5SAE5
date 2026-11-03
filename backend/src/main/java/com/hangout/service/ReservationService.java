package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.config.SecurityUtils;
import com.hangout.dto.EventRegistrationRequest;
import com.hangout.dto.ReservationRequest;
import com.hangout.dto.ReservationResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Event;
import com.hangout.model.MenuItem;
import com.hangout.model.Notification;
import com.hangout.model.PointTransaction;
import com.hangout.model.Reservation;
import com.hangout.model.Reservation.ReservationStatus;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.EventRepository;
import com.hangout.repository.MenuItemRepository;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private static final int POINTS_FOR_HONORED_RESERVATION = 20;
    private static final double TAX_RATE = 0.08;

    /** A confirmed guest who hasn't been checked in this long after the reserved time is cancelled automatically. */
    @Value("${reservation.grace-minutes:30}")
    private int graceMinutes;

    /** Don't notify guests about lateness that is older than this (e.g. stale data from before this feature). */
    private static final int MAX_NOTIFY_AGE_HOURS = 24;

    private String lateNote() {
        return "Please note: your reservation will be cancelled automatically if you are more than "
            + graceMinutes + " minutes late.";
    }

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM 'at' HH:mm", Locale.ENGLISH);

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final EventRepository eventRepository;
    private final LoyaltyService loyaltyService;
    private final NotificationService notificationService;

    public List<ReservationResponse> getUserReservations(String userId) {
        return reservationRepository.findByUserId(userId).stream()
            .sorted(Newest.first(Reservation::getId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<ReservationResponse> getRestaurantReservations(String restaurantId) {
        return reservationRepository.findByRestaurantId(restaurantId).stream()
            .sorted(Newest.first(Reservation::getId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public Reservation getReservationById(String id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    public ReservationResponse getResponseById(String id) {
        return toResponse(getReservationById(id));
    }

    public void assertCanView(Reservation reservation) {
        String userId = SecurityUtils.getCurrentUserId();
        if (reservation.getUserId().equals(userId)) return;
        if (SecurityUtils.hasRole("ADMIN")) return;
        if (SecurityUtils.hasRole("RESTAURANT_OWNER") && isOwnerOfRestaurant(userId, reservation.getRestaurantId())) return;
        throw new AccessDeniedException("You cannot view this reservation");
    }

    private boolean isOwnerOfRestaurant(String userId, String restaurantId) {
        Restaurant restaurant = restaurantId == null ? null : restaurantRepository.findById(restaurantId).orElse(null);
        return restaurant != null && userId.equals(restaurant.getOwnerId());
    }

    // ------------------------------------------------------------------
    // Creating requests
    // ------------------------------------------------------------------

    @Transactional
    public ReservationResponse createReservation(String userId, ReservationRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setRestaurantId(request.getRestaurantId());
        reservation.setContactName(request.getContactName().trim());
        reservation.setContactPhone(request.getContactPhone().trim());
        reservation.setReservationDateTime(request.getReservationDateTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setSpecialRequests(request.getSpecialRequests());
        reservation.setStatus(ReservationStatus.PENDING);

        applyItems(reservation, request.getItems());

        Reservation saved = reservationRepository.save(reservation);
        countReservationFor(userId);

        notifyOwnerOfNewRequest(restaurant, saved,
            saved.getContactName() + " requested a table for " + saved.getPartySize()
                + " on " + saved.getReservationDateTime().format(WHEN) + ".");

        return toResponse(saved);
    }

    /**
     * Registers guests for an event as a request awaiting the restaurant owner's
     * answer, exactly like a table booking. The spots were already held by
     * {@code EventService.reserveSpots} before this is called.
     */
    @Transactional
    public ReservationResponse createForEvent(String userId, Event event, EventRegistrationRequest request) {
        Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setRestaurantId(event.getRestaurantId());
        reservation.setEventId(event.getId());
        reservation.setContactName(request.getContactName().trim());
        reservation.setContactPhone(request.getContactPhone().trim());
        reservation.setReservationDateTime(event.getEventDate());
        reservation.setPartySize(request.getPartySize());
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = reservationRepository.save(reservation);
        countReservationFor(userId);

        notifyOwnerOfNewRequest(restaurant, saved,
            saved.getContactName() + " registered " + saved.getPartySize()
                + (saved.getPartySize() == 1 ? " person" : " people") + " for \"" + event.getTitle() + "\".");

        return toResponse(saved);
    }

    private static final Set<ReservationStatus> INACTIVE = Set.of(
        ReservationStatus.CANCELLED, ReservationStatus.REFUSED, ReservationStatus.LATE
    );

    public void assertNotAlreadyRegisteredForEvent(String userId, String eventId) {
        boolean alreadyRegistered = reservationRepository.findByUserIdAndEventId(userId, eventId).stream()
            .anyMatch(r -> !INACTIVE.contains(r.getStatus()));
        if (alreadyRegistered) {
            throw new BadRequestException("You have already registered for this event");
        }
    }

    private void countReservationFor(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setTotalReservations(user.getTotalReservations() + 1);
        userRepository.save(user);
    }

    private void applyItems(Reservation reservation, List<ReservationRequest.ItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return;
        }

        List<Reservation.ReservationItem> items = new ArrayList<>();
        double subtotal = 0.0;

        for (ReservationRequest.ItemRequest itemRequest : itemRequests) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemRequest.getMenuItemId()));

            Reservation.ReservationItem item = new Reservation.ReservationItem();
            item.setMenuItemId(menuItem.getId());
            item.setName(menuItem.getName());
            item.setPrice(menuItem.getPrice());
            item.setQuantity(itemRequest.getQuantity());
            items.add(item);

            subtotal += menuItem.getPrice() * itemRequest.getQuantity();
        }

        double tax = Math.round(subtotal * TAX_RATE * 100.0) / 100.0;

        reservation.setItems(items);
        reservation.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        reservation.setTax(tax);
        reservation.setTotal(Math.round((subtotal + tax) * 100.0) / 100.0);
    }

    // ------------------------------------------------------------------
    // Guest cancels
    // ------------------------------------------------------------------

    @Transactional
    public void cancelOwnReservation(String id, String userId) {
        Reservation reservation = getReservationById(id);
        if (!reservation.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own reservations");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("This reservation can no longer be cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        releaseEventSpotsIfNeeded(reservation);
    }

    // ------------------------------------------------------------------
    // Owner decides: refuse / accept / arrived
    // ------------------------------------------------------------------

    @Transactional
    public ReservationResponse updateStatus(String id, String restaurantOwnerId, ReservationStatus newStatus) {
        Reservation reservation = getReservationById(id);
        if (!isOwnerOfRestaurant(restaurantOwnerId, reservation.getRestaurantId())) {
            throw new AccessDeniedException("You do not manage this restaurant");
        }

        ReservationStatus current = reservation.getStatus();

        if (isOverdue(reservation)) {
            markLate(reservation);
            throw new BadRequestException(
                "The guest was more than " + graceMinutes + " minutes late - this reservation was cancelled automatically.");
        }

        switch (newStatus) {
            case CONFIRMED -> require(current == ReservationStatus.PENDING, "Only a pending request can be accepted");
            case REFUSED -> require(current == ReservationStatus.PENDING || current == ReservationStatus.CONFIRMED,
                "This reservation can no longer be refused");
            case ARRIVED -> require(current == ReservationStatus.CONFIRMED, "Accept the reservation before marking the guest as arrived");
            case COMPLETED -> require(current == ReservationStatus.CONFIRMED || current == ReservationStatus.ARRIVED,
                "This reservation cannot be completed");
            case CANCELLED -> require(current == ReservationStatus.PENDING || current == ReservationStatus.CONFIRMED,
                "This reservation can no longer be cancelled");
            default -> throw new BadRequestException("Status " + newStatus + " cannot be set manually");
        }

        reservation.setStatus(newStatus);
        reservationRepository.save(reservation);

        String restaurantName = restaurantName(reservation);
        String when = reservation.getReservationDateTime() != null ? reservation.getReservationDateTime().format(WHEN) : "";

        switch (newStatus) {
            case CONFIRMED -> notifyGuest(reservation, "Reservation accepted",
                restaurantName + " accepted your " + kind(reservation) + " for " + when + ". " + lateNote());
            case REFUSED -> {
                releaseEventSpotsIfNeeded(reservation);
                notifyGuest(reservation, "Reservation declined",
                    restaurantName + " could not accept your " + kind(reservation) + " for " + when + ".");
            }
            case CANCELLED -> {
                releaseEventSpotsIfNeeded(reservation);
                notifyGuest(reservation, "Reservation cancelled",
                    restaurantName + " cancelled your " + kind(reservation) + " for " + when + ".");
            }
            case ARRIVED -> {
                notifyGuest(reservation, "Welcome!",
                    "Your arrival at " + restaurantName + " is confirmed - your reservation is complete. Enjoy!");
                awardPoints(reservation);
            }
            case COMPLETED -> {
                notifyGuest(reservation, "Reservation completed", "Thanks for visiting " + restaurantName + "!");
                if (current != ReservationStatus.ARRIVED) {
                    awardPoints(reservation);
                }
            }
            default -> { }
        }

        return toResponse(reservation);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }

    private void awardPoints(Reservation reservation) {
        loyaltyService.awardPoints(
            reservation.getUserId(),
            POINTS_FOR_HONORED_RESERVATION,
            PointTransaction.TransactionType.EARN_RESERVATION,
            "Reservation honored",
            reservation.getId()
        );
    }

    // ------------------------------------------------------------------
    // Automatic lateness cancellation
    // ------------------------------------------------------------------

    private boolean isOverdue(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CONFIRMED
            && reservation.getReservationDateTime() != null
            && LocalDateTime.now().isAfter(reservation.getReservationDateTime().plusMinutes(graceMinutes));
    }

    /** Cancels every accepted reservation whose guest hasn't arrived within the grace period. */
    @Transactional
    public int cancelLateReservations() {
        int count = 0;
        for (Reservation reservation : reservationRepository.findByStatus(ReservationStatus.CONFIRMED)) {
            if (isOverdue(reservation)) {
                markLate(reservation);
                count++;
            }
        }
        if (count > 0) {
            log.info("Cancelled {} reservation(s) because the guest was more than {} minutes late", count, graceMinutes);
        }
        return count;
    }

    private void markLate(Reservation reservation) {
        reservation.setStatus(ReservationStatus.LATE);
        reservationRepository.save(reservation);
        releaseEventSpotsIfNeeded(reservation);

        boolean recent = LocalDateTime.now().isBefore(
            reservation.getReservationDateTime().plusMinutes(graceMinutes).plusHours(MAX_NOTIFY_AGE_HOURS));
        if (recent) {
            notifyGuest(reservation, "Reservation cancelled - you were late",
                "Your " + kind(reservation) + " at " + restaurantName(reservation)
                    + " was cancelled automatically because you were more than " + graceMinutes + " minutes late.");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void releaseEventSpotsIfNeeded(Reservation reservation) {
        if (reservation.getEventId() != null) {
            int people = reservation.getPartySize() != null ? reservation.getPartySize() : 1;
            eventRepository.findById(reservation.getEventId()).ifPresent(event -> {
                event.setBookedSpots(Math.max(0, event.getBookedSpots() - people));
                eventRepository.save(event);
            });
        }
    }

    private String kind(Reservation reservation) {
        return reservation.getEventId() != null ? "event registration" : "reservation";
    }

    private String restaurantName(Reservation reservation) {
        Restaurant restaurant = reservation.getRestaurantId() != null
            ? restaurantRepository.findById(reservation.getRestaurantId()).orElse(null)
            : null;
        return restaurant != null ? restaurant.getName() : "the restaurant";
    }

    private Notification.NotificationType notificationType(Reservation reservation) {
        return reservation.getEventId() != null
            ? Notification.NotificationType.EVENT
            : Notification.NotificationType.RESERVATION;
    }

    private void notifyGuest(Reservation reservation, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(reservation.getUserId());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(notificationType(reservation));
        notification.setRelatedId(reservation.getId());
        notificationService.createNotification(notification);
    }

    private void notifyOwnerOfNewRequest(Restaurant restaurant, Reservation reservation, String message) {
        if (restaurant.getOwnerId() == null) return;

        Notification notification = new Notification();
        notification.setUserId(restaurant.getOwnerId());
        notification.setTitle(reservation.getEventId() != null ? "New event registration" : "New reservation request");
        notification.setMessage(message);
        notification.setType(notificationType(reservation));
        notification.setRelatedId(reservation.getId());
        notificationService.createNotification(notification);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        Restaurant restaurant = reservation.getRestaurantId() != null
            ? restaurantRepository.findById(reservation.getRestaurantId()).orElse(null)
            : null;
        Event event = reservation.getEventId() != null
            ? eventRepository.findById(reservation.getEventId()).orElse(null)
            : null;

        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setUserId(reservation.getUserId());
        response.setRestaurantId(reservation.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setEventId(reservation.getEventId());
        response.setEventTitle(event != null ? event.getTitle() : null);
        response.setContactName(reservation.getContactName());
        response.setContactPhone(reservation.getContactPhone());
        response.setReservationDateTime(reservation.getReservationDateTime());
        response.setPartySize(reservation.getPartySize());
        response.setSpecialRequests(reservation.getSpecialRequests());
        response.setStatus(reservation.getStatus().name());
        response.setItems(reservation.getItems());
        response.setSubtotal(reservation.getSubtotal());
        response.setTax(reservation.getTax());
        response.setTotal(reservation.getTotal());
        response.setCreatedAt(reservation.getCreatedAt());
        return response;
    }
}
