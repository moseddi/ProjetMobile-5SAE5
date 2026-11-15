package com.hangout.service;

import com.hangout.dto.OwnerApplicationRequest;
import com.hangout.dto.OwnerApplicationResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Notification;
import com.hangout.model.OwnerApplication;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.OwnerApplicationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerApplicationService {

    private final OwnerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /** Public: a restaurant owner asks to join. Creates nothing but the request. */
    public OwnerApplicationResponse submit(OwnerApplicationRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email) || userRepository.existsByEmail(request.getEmail().trim())) {
            throw new BadRequestException("This email already has an account. Please log in instead.");
        }
        if (applicationRepository.existsByEmailIgnoreCaseAndStatus(email, OwnerApplication.Status.PENDING)) {
            throw new BadRequestException("A request for this email is already waiting for admin approval.");
        }

        OwnerApplication a = new OwnerApplication();
        a.setOwnerName(request.getOwnerName().trim());
        a.setEmail(email);
        a.setPhone(request.getPhone().trim());
        a.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        a.setRestaurantName(request.getRestaurantName().trim());
        a.setCuisine(request.getCuisine().trim());
        a.setDescription(request.getDescription().trim());
        a.setImage(request.getImage() != null ? request.getImage().trim() : "");
        a.setMenuImage(request.getMenuImage());
        a.setAddress(request.getAddress().trim());
        a.setRestaurantPhone(request.getRestaurantPhone() != null && !request.getRestaurantPhone().isBlank()
            ? request.getRestaurantPhone().trim() : request.getPhone().trim());
        a.setOpeningHours(request.getOpeningHours() != null ? request.getOpeningHours().trim() : "");
        a.setPriceRange(request.getPriceRange() != null ? request.getPriceRange() : Restaurant.PriceRange.MODERATE);
        a.setStatus(OwnerApplication.Status.PENDING);

        OwnerApplication saved = applicationRepository.save(a);

        // Let every admin know a request is waiting.
        userRepository.findByRole(User.Role.ADMIN).forEach(admin -> {
            Notification n = new Notification();
            n.setUserId(admin.getId());
            n.setTitle("New restaurant request");
            n.setMessage(saved.getOwnerName() + " asked to join with \"" + saved.getRestaurantName() + "\".");
            n.setType(Notification.NotificationType.SYSTEM);
            n.setRelatedId(saved.getId());
            notificationService.createNotification(n);
        });

        return OwnerApplicationResponse.from(saved);
    }

    public List<OwnerApplicationResponse> list(OwnerApplication.Status status) {
        List<OwnerApplication> all = status != null
            ? applicationRepository.findByStatus(status)
            : applicationRepository.findAll();
        return all.stream()
            .sorted(Comparator.comparing(OwnerApplication::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .map(OwnerApplicationResponse::from)
            .collect(Collectors.toList());
    }

    public long pendingCount() {
        return applicationRepository.countByStatus(OwnerApplication.Status.PENDING);
    }

    /** Accepting creates the owner account and the restaurant, linked together. */
    public OwnerApplicationResponse accept(String id) {
        OwnerApplication a = getPending(id);

        if (userRepository.existsByEmail(a.getEmail())) {
            throw new BadRequestException("An account with " + a.getEmail()
                + " already exists, so this request cannot be accepted. Reject it or ask the owner to use another email.");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(a.getRestaurantName());
        restaurant.setCuisine(a.getCuisine());
        restaurant.setDescription(a.getDescription());
        restaurant.setImage(a.getImage());
        restaurant.setMenuImage(a.getMenuImage());
        restaurant.setAddress(a.getAddress());
        restaurant.setPhone(a.getRestaurantPhone());
        restaurant.setOpeningHours(a.getOpeningHours());
        restaurant.setPriceRange(a.getPriceRange());
        restaurant.setIsOpen(true);
        restaurant.setRating(0.0);
        restaurant.setReviewCount(0);
        restaurant = restaurantRepository.save(restaurant);

        User owner = new User();
        owner.setEmail(a.getEmail());
        owner.setName(a.getOwnerName());
        owner.setPhone(a.getPhone());
        owner.setAvatar("");
        owner.setPassword(a.getPasswordHash());
        owner.setRole(User.Role.RESTAURANT_OWNER);
        owner.setRestaurantId(restaurant.getId());
        owner.setActive(true);
        owner = userRepository.save(owner);

        restaurant.setOwnerId(owner.getId());
        restaurantRepository.save(restaurant);

        a.setStatus(OwnerApplication.Status.ACCEPTED);
        a.setReviewedAt(LocalDateTime.now());
        a.setCreatedUserId(owner.getId());
        a.setCreatedRestaurantId(restaurant.getId());
        a.setRejectionReason(null);
        // The hash now lives on the user; no need to keep a second copy.
        a.setPasswordHash(null);

        // E-mail is intentionally not implemented yet: this in-app notification stands in for it.
        Notification n = new Notification();
        n.setUserId(owner.getId());
        n.setTitle("Welcome to HangOut!");
        n.setMessage("Your restaurant \"" + restaurant.getName() + "\" was approved. You can now manage it.");
        n.setType(Notification.NotificationType.SYSTEM);
        n.setRelatedId(restaurant.getId());
        notificationService.createNotification(n);

        return OwnerApplicationResponse.from(applicationRepository.save(a));
    }

    public OwnerApplicationResponse reject(String id, String reason) {
        OwnerApplication a = getPending(id);
        a.setStatus(OwnerApplication.Status.REJECTED);
        a.setReviewedAt(LocalDateTime.now());
        a.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        a.setPasswordHash(null);
        return OwnerApplicationResponse.from(applicationRepository.save(a));
    }

    private OwnerApplication getPending(String id) {
        OwnerApplication a = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (a.getStatus() != OwnerApplication.Status.PENDING) {
            throw new BadRequestException("This request was already " + a.getStatus().name().toLowerCase());
        }
        return a;
    }
}
