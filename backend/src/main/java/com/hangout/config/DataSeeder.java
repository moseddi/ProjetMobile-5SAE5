package com.hangout.config;

import com.hangout.model.*;
import com.hangout.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds demo/test accounts and sample data. Designed to be safe to run against a
 * database that already has some data in it (e.g. from before roles existed): each
 * named seed record is created only if missing, rather than bailing out entirely the
 * moment any user exists. Re-running this never duplicates records.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final HangOutRepository hangOutRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Checking seed data...");

        Restaurant leGourmet = ensureRestaurant(seedLeGourmet());
        Restaurant sushiParadise = ensureRestaurant(seedSushiParadise());
        Restaurant bellaItalia = ensureRestaurant(seedBellaItalia());

        User admin = ensureUser("admin@hangout.com", "admin123", "Platform Admin",
            "+1-555-0001", "https://i.pravatar.cc/150?img=12", User.Role.ADMIN, null);

        User ownerLeGourmet = ensureUser("owner.legourmet@hangout.com", "owner123", "Marie Dubois",
            "+1-555-0101", "https://i.pravatar.cc/150?img=5", User.Role.RESTAURANT_OWNER, leGourmet.getId());
        linkOwner(leGourmet, ownerLeGourmet.getId());

        User ownerSushi = ensureUser("owner.sushi@hangout.com", "owner123", "Kenji Tanaka",
            "+1-555-0102", "https://i.pravatar.cc/150?img=6", User.Role.RESTAURANT_OWNER, sushiParadise.getId());
        linkOwner(sushiParadise, ownerSushi.getId());

        User demo = ensureUser("demo@example.com", "123456", "Demo User",
            "+1234567890", "https://i.pravatar.cc/150?img=1", User.Role.CLIENT, null);
        User john = ensureUser("john@example.com", "password", "John Smith",
            "+1234567891", "https://i.pravatar.cc/150?img=2", User.Role.CLIENT, null);
        User emily = ensureUser("emily@example.com", "password", "Emily Chen",
            "+1234567892", "https://i.pravatar.cc/150?img=3", User.Role.CLIENT, null);

        ensureMenuItems(leGourmet, sushiParadise);
        ensureReviews(leGourmet, sushiParadise, demo, john);
        ensureEvents(leGourmet);
        ensureHangOuts(leGourmet, demo, john);

        log.info("Seed data check complete.");
        log.info("=================================================================");
        log.info(" Demo accounts (all passwords shown below):");
        log.info("   ADMIN            -> admin@hangout.com / admin123");
        log.info("   RESTAURANT_OWNER -> owner.legourmet@hangout.com / owner123  (Le Gourmet)");
        log.info("   RESTAURANT_OWNER -> owner.sushi@hangout.com / owner123     (Sushi Paradise)");
        log.info("   CLIENT           -> demo@example.com / 123456");
        log.info("   CLIENT           -> john@example.com / password");
        log.info("   CLIENT           -> emily@example.com / password");
        log.info("=================================================================");
    }

    private User ensureUser(String email, String rawPassword, String name, String phone,
                             String avatar, User.Role role, String restaurantId) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Seeding user: {}", email);
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setName(name);
            user.setPhone(phone);
            user.setAvatar(avatar);
            user.setRole(role);
            user.setRestaurantId(restaurantId);
            user.setActive(true);
            user.setLoyaltyPoints(0);
            user.setTotalReservations(0);
            user.setTotalOrders(0);
            user.setTotalReviews(0);
            return userRepository.save(user);
        });
    }

    private void linkOwner(Restaurant restaurant, String ownerId) {
        if (ownerId != null && !ownerId.equals(restaurant.getOwnerId())) {
            restaurant.setOwnerId(ownerId);
            restaurantRepository.save(restaurant);
        }
    }

    private Restaurant ensureRestaurant(Restaurant template) {
        return restaurantRepository.findByName(template.getName()).orElseGet(() -> {
            log.info("Seeding restaurant: {}", template.getName());
            return restaurantRepository.save(template);
        });
    }

    private Restaurant seedLeGourmet() {
        Restaurant r = new Restaurant();
        r.setName("Le Gourmet");
        r.setCuisine("French");
        r.setDescription("Exquisite French cuisine with a modern twist");
        r.setImage("https://images.unsplash.com/photo-1414235077428-338989a2e8c0");
        r.setAddress("123 Main St, Downtown");
        r.setPhone("+1-555-0101");
        r.setOpeningHours("11:00 AM - 10:00 PM");
        r.setRating(4.5);
        r.setReviewCount(0);
        r.setIsOpen(true);
        r.setFeatures(Arrays.asList("Fine Dining", "Wine Bar", "Outdoor Seating"));
        r.setPriceRange(Restaurant.PriceRange.EXPENSIVE);
        return r;
    }

    private Restaurant seedSushiParadise() {
        Restaurant r = new Restaurant();
        r.setName("Sushi Paradise");
        r.setCuisine("Japanese");
        r.setDescription("Authentic Japanese sushi and sashimi");
        r.setImage("https://images.unsplash.com/photo-1579584425555-c3ce17fd4351");
        r.setAddress("456 Oak Ave, Midtown");
        r.setPhone("+1-555-0102");
        r.setOpeningHours("12:00 PM - 11:00 PM");
        r.setRating(4.7);
        r.setReviewCount(0);
        r.setIsOpen(true);
        r.setFeatures(Arrays.asList("Sushi Bar", "Sake Selection", "Takeout"));
        r.setPriceRange(Restaurant.PriceRange.EXPENSIVE);
        return r;
    }

    private Restaurant seedBellaItalia() {
        Restaurant r = new Restaurant();
        r.setName("Bella Italia");
        r.setCuisine("Italian");
        r.setDescription("Traditional Italian pasta and pizza");
        r.setImage("https://images.unsplash.com/photo-1555396273-367ea4eb4db5");
        r.setAddress("789 Pine Rd, Little Italy");
        r.setPhone("+1-555-0103");
        r.setOpeningHours("11:30 AM - 10:30 PM");
        r.setRating(4.3);
        r.setReviewCount(0);
        r.setIsOpen(true);
        r.setFeatures(Arrays.asList("Family Friendly", "Pizza Oven", "Wine List"));
        r.setPriceRange(Restaurant.PriceRange.MODERATE);
        // Intentionally left unassigned (no owner) to exercise the admin-managed case.
        return r;
    }

    private void ensureMenuItems(Restaurant leGourmet, Restaurant sushiParadise) {
        if (!menuItemRepository.findByRestaurantId(leGourmet.getId()).isEmpty()) {
            return;
        }

        log.info("Seeding menu items");
        List<MenuItem> items = new ArrayList<>();

        MenuItem m1 = new MenuItem();
        m1.setRestaurantId(leGourmet.getId());
        m1.setName("Foie Gras");
        m1.setDescription("Pan-seared foie gras with fig compote");
        m1.setImage("https://images.unsplash.com/photo-1544025162-d76694265947");
        m1.setPrice(28.00);
        m1.setCategory("STARTERS");
        items.add(m1);

        MenuItem m2 = new MenuItem();
        m2.setRestaurantId(leGourmet.getId());
        m2.setName("Coq au Vin");
        m2.setDescription("Braised chicken in red wine sauce");
        m2.setImage("https://images.unsplash.com/photo-1598103442097-8b74394b95c6");
        m2.setPrice(42.00);
        m2.setCategory("MAIN_COURSES");
        items.add(m2);

        MenuItem m3 = new MenuItem();
        m3.setRestaurantId(sushiParadise.getId());
        m3.setName("Sashimi Platter");
        m3.setDescription("Assorted fresh sashimi");
        m3.setImage("https://images.unsplash.com/photo-1579584425555-c3ce17fd4351");
        m3.setPrice(35.00);
        m3.setCategory("MAIN_COURSES");
        items.add(m3);

        menuItemRepository.saveAll(items);
    }

    private void ensureReviews(Restaurant leGourmet, Restaurant sushiParadise, User demo, User john) {
        if (!reviewRepository.findByRestaurantId(leGourmet.getId()).isEmpty()) {
            return;
        }

        log.info("Seeding reviews");
        List<Review> reviews = new ArrayList<>();

        Review r1 = new Review();
        r1.setUserId(demo.getId());
        r1.setRestaurantId(leGourmet.getId());
        r1.setRating(5.0);
        r1.setTitle("Exceptional French cuisine");
        r1.setComment("The coq au vin was absolutely divine. Best French restaurant in the city!");
        r1.setHelpfulCount(12);
        r1.setStatus(Review.ReviewStatus.PUBLISHED);
        reviews.add(r1);

        Review r2 = new Review();
        r2.setUserId(john.getId());
        r2.setRestaurantId(sushiParadise.getId());
        r2.setRating(5.0);
        r2.setTitle("Best sushi in town");
        r2.setComment("Fresh fish, excellent presentation. The chef is a true master!");
        r2.setHelpfulCount(8);
        r2.setStatus(Review.ReviewStatus.PUBLISHED);
        reviews.add(r2);

        reviewRepository.saveAll(reviews);
    }

    private void ensureEvents(Restaurant leGourmet) {
        if (!eventRepository.findByRestaurantId(leGourmet.getId()).isEmpty()) {
            return;
        }

        log.info("Seeding events");
        Event e1 = new Event();
        e1.setRestaurantId(leGourmet.getId());
        e1.setTitle("Wine Tasting Evening");
        e1.setDescription("Exclusive wine tasting with our sommelier");
        e1.setImage("https://images.unsplash.com/photo-1510812431401-41d2bd2722f3");
        e1.setEventDate(LocalDateTime.now().plusDays(7));
        e1.setPrice(75.00);
        e1.setAvailableSpots(20);
        e1.setBookedSpots(5);
        e1.setCategory("WINE_TASTING");

        eventRepository.save(e1);
    }

    private void ensureHangOuts(Restaurant leGourmet, User demo, User john) {
        if (!hangOutRepository.findByRestaurantId(leGourmet.getId()).isEmpty()) {
            return;
        }

        log.info("Seeding hangouts");
        HangOut h1 = new HangOut();
        h1.setCreatorId(demo.getId());
        h1.setRestaurantId(leGourmet.getId());
        h1.setTitle("Friday Night Dinner");
        h1.setDescription("Let's try the new French restaurant!");
        h1.setPlannedDateTime(LocalDateTime.now().plusDays(5));
        h1.setMaxParticipants(6);
        h1.setStatus(HangOut.HangOutStatus.OPEN);

        List<HangOut.Participant> participants = new ArrayList<>();
        HangOut.Participant p1 = new HangOut.Participant();
        p1.setId(UUID.randomUUID().toString());
        p1.setUserId(john.getId());
        p1.setStatus(HangOut.ParticipantStatus.ACCEPTED);
        p1.setRequestedAt(LocalDateTime.now().minusDays(1));
        participants.add(p1);
        h1.setParticipants(participants);

        hangOutRepository.save(h1);
    }
}
