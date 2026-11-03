package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.dto.AdminStats;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.EventRepository;
import com.hangout.repository.HangOutRepository;
import com.hangout.repository.ReservationRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.dto.RestaurantWithOwnerRequest;
import com.hangout.repository.OwnerApplicationRepository;
import com.hangout.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final HangOutRepository hangOutRepository;
    private final EventRepository eventRepository;
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getUsers(User.Role roleFilter) {
        List<User> users = roleFilter != null ? userRepository.findByRole(roleFilter) : userRepository.findAll();
        return users.stream().sorted(Newest.first(User::getId)).collect(java.util.stream.Collectors.toList());
    }

    public User getUser(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User setActive(String userId, boolean active) {
        User user = getUser(userId);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String userId) {
        User user = getUser(userId);
        if (user.getRole() == User.Role.RESTAURANT_OWNER && user.getRestaurantId() != null) {
            restaurantRepository.findById(user.getRestaurantId()).ifPresent(r -> {
                r.setOwnerId(null);
                restaurantRepository.save(r);
            });
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public User assignOwner(String userId, String restaurantId) {
        User user = getUser(userId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (restaurant.getOwnerId() != null && !restaurant.getOwnerId().equals(userId)) {
            throw new BadRequestException("This restaurant already has an owner. Unassign them first.");
        }

        // If this user already owned a different restaurant, free it up.
        if (user.getRestaurantId() != null && !user.getRestaurantId().equals(restaurantId)) {
            restaurantRepository.findById(user.getRestaurantId()).ifPresent(previous -> {
                previous.setOwnerId(null);
                restaurantRepository.save(previous);
            });
        }

        user.setRole(User.Role.RESTAURANT_OWNER);
        user.setRestaurantId(restaurantId);
        userRepository.save(user);

        restaurant.setOwnerId(userId);
        restaurantRepository.save(restaurant);

        return user;
    }

    @Transactional
    public User unassignOwner(String userId) {
        User user = getUser(userId);

        if (user.getRestaurantId() != null) {
            restaurantRepository.findById(user.getRestaurantId()).ifPresent(restaurant -> {
                restaurant.setOwnerId(null);
                restaurantRepository.save(restaurant);
            });
        }

        user.setRole(User.Role.CLIENT);
        user.setRestaurantId(null);
        return userRepository.save(user);
    }

    public User setRole(String userId, User.Role role) {
        if (role == User.Role.RESTAURANT_OWNER) {
            throw new BadRequestException("Use the assign-owner endpoint to make a user a restaurant owner");
        }
        User user = getUser(userId);

        if (user.getRole() == User.Role.RESTAURANT_OWNER && user.getRestaurantId() != null) {
            restaurantRepository.findById(user.getRestaurantId()).ifPresent(restaurant -> {
                restaurant.setOwnerId(null);
                restaurantRepository.save(restaurant);
            });
            user.setRestaurantId(null);
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    /** Creates a restaurant and, when requested, its owner (new account or an existing user) in one step. */
    @Transactional
    public Restaurant createRestaurantWithOwner(RestaurantWithOwnerRequest request) {
        Restaurant restaurant = request.getRestaurant();
        if (restaurant == null || restaurant.getName() == null || restaurant.getName().isBlank()) {
            throw new BadRequestException("Restaurant name is required");
        }

        User owner = null;
        RestaurantWithOwnerRequest.NewOwner newOwner = request.getOwner();
        if (newOwner != null) {
            if (newOwner.getName() == null || newOwner.getName().isBlank()
                || newOwner.getEmail() == null || !newOwner.getEmail().contains("@")
                || newOwner.getPassword() == null || newOwner.getPassword().length() < 6) {
                throw new BadRequestException("Owner name, a valid email and a password of at least 6 characters are required");
            }
            String email = newOwner.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmail(email) || userRepository.existsByEmail(newOwner.getEmail().trim())) {
                throw new BadRequestException("An account with this email already exists. Choose the existing account option instead.");
            }
            owner = new User();
            owner.setEmail(email);
            owner.setName(newOwner.getName().trim());
            owner.setPhone(newOwner.getPhone() != null ? newOwner.getPhone().trim() : "");
            owner.setAvatar("");
            owner.setPassword(passwordEncoder.encode(newOwner.getPassword()));
            owner.setActive(true);
        } else if (request.getExistingOwnerId() != null && !request.getExistingOwnerId().isBlank()) {
            owner = getUser(request.getExistingOwnerId());
            if (owner.getRole() == User.Role.ADMIN) {
                throw new BadRequestException("An admin account cannot be made a restaurant owner");
            }
        }

        restaurant.setId(null);
        restaurant.setOwnerId(null);
        restaurant = restaurantRepository.save(restaurant);

        if (owner != null) {
            if (owner.getId() == null) {
                owner.setRole(User.Role.RESTAURANT_OWNER);
                owner.setRestaurantId(restaurant.getId());
                owner = userRepository.save(owner);
                restaurant.setOwnerId(owner.getId());
                restaurant = restaurantRepository.save(restaurant);
            } else {
                assignOwner(owner.getId(), restaurant.getId());
                restaurant = restaurantRepository.findById(restaurant.getId()).orElse(restaurant);
            }
        }
        return restaurant;
    }

    public AdminStats getStats() {
        AdminStats stats = new AdminStats();
        List<User> users = userRepository.findAll();
        stats.setTotalUsers(users.size());
        stats.setTotalClients(users.stream().filter(u -> u.getRole() == User.Role.CLIENT).count());
        stats.setTotalRestaurantOwners(users.stream().filter(u -> u.getRole() == User.Role.RESTAURANT_OWNER).count());
        stats.setTotalAdmins(users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count());

        List<Restaurant> restaurants = restaurantRepository.findAll();
        stats.setTotalRestaurants(restaurants.size());
        stats.setUnassignedRestaurants(restaurants.stream().filter(r -> r.getOwnerId() == null).count());

        stats.setTotalReservations(reservationRepository.count());
        stats.setTotalReviews(reviewRepository.count());
        stats.setTotalHangOuts(hangOutRepository.count());
        stats.setTotalEvents(eventRepository.count());
        stats.setPendingOwnerApplications(
            ownerApplicationRepository.countByStatus(com.hangout.model.OwnerApplication.Status.PENDING));

        return stats;
    }
}
