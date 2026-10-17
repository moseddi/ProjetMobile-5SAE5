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
    private final QuizQuestionRepository quizQuestionRepository;
    private final RewardRepository rewardRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding database...");

            // Seed Users
            List<User> users = seedUsers();

            // Seed Restaurants
            List<Restaurant> restaurants = seedRestaurants();

            // Seed Menu Items
            seedMenuItems(restaurants);

            // Seed Reviews
            seedReviews(users, restaurants);

            // Seed Events
            seedEvents(restaurants);

            // Seed HangOuts
            seedHangOuts(users, restaurants);

            log.info("Database seeding completed!");
        } else {
            log.info("Database already seeded, skipping core seed data...");
        }

        // Loyalty seed data is independent of the core seed guard above, so it also
        // backfills a database that was seeded before the loyalty feature existed.
        if (quizQuestionRepository.count() == 0) {
            seedQuizQuestions();
            log.info("Seeded quiz questions.");
        }
        if (rewardRepository.count() == 0) {
            seedRewards();
            log.info("Seeded rewards catalog.");
        }
        if (rewardRepository.findByActiveTrueAndRestaurantIdNotNull().isEmpty()) {
            seedRestaurantRewards();
            log.info("Seeded restaurant-specific rewards.");
        }
    }
    
    private List<User> seedUsers() {
        User demo = new User();
        demo.setEmail("demo@example.com");
        demo.setPassword(passwordEncoder.encode("123456"));
        demo.setName("Demo User");
        demo.setPhone("+1234567890");
        demo.setAvatar("https://i.pravatar.cc/150?img=1");
        demo.setLoyaltyPoints(250);
        demo.setTotalReservations(5);
        demo.setTotalOrders(3);
        demo.setTotalReviews(2);
        
        User john = new User();
        john.setEmail("john@example.com");
        john.setPassword(passwordEncoder.encode("password"));
        john.setName("John Smith");
        john.setPhone("+1234567891");
        john.setAvatar("https://i.pravatar.cc/150?img=2");
        john.setLoyaltyPoints(150);
        
        User emily = new User();
        emily.setEmail("emily@example.com");
        emily.setPassword(passwordEncoder.encode("password"));
        emily.setName("Emily Chen");
        emily.setPhone("+1234567892");
        emily.setAvatar("https://i.pravatar.cc/150?img=3");
        emily.setLoyaltyPoints(300);
        
        List<User> users = Arrays.asList(demo, john, emily);
        return userRepository.saveAll(users);
    }
    
    private List<Restaurant> seedRestaurants() {
        Restaurant r1 = new Restaurant();
        r1.setName("Le Gourmet");
        r1.setCuisine("French");
        r1.setDescription("Exquisite French cuisine with a modern twist");
        r1.setImage("https://images.unsplash.com/photo-1414235077428-338989a2e8c0");
        r1.setAddress("123 Main St, Downtown");
        r1.setPhone("+1-555-0101");
        r1.setSeatingCapacity(40);
        r1.setOpeningHours("11:00 AM - 10:00 PM");
        r1.setRating(4.5);
        r1.setReviewCount(0);
        r1.setIsOpen(true);
        r1.setFeatures(Arrays.asList("Fine Dining", "Wine Bar", "Outdoor Seating"));
        r1.setPriceRange(Restaurant.PriceRange.EXPENSIVE);
        
        Restaurant r2 = new Restaurant();
        r2.setName("Sushi Paradise");
        r2.setCuisine("Japanese");
        r2.setDescription("Authentic Japanese sushi and sashimi");
        r2.setImage("https://images.unsplash.com/photo-1579584425555-c3ce17fd4351");
        r2.setAddress("456 Oak Ave, Midtown");
        r2.setPhone("+1-555-0102");
        r2.setSeatingCapacity(30);
        r2.setOpeningHours("12:00 PM - 11:00 PM");
        r2.setRating(4.7);
        r2.setReviewCount(0);
        r2.setIsOpen(true);
        r2.setFeatures(Arrays.asList("Sushi Bar", "Sake Selection", "Takeout"));
        r2.setPriceRange(Restaurant.PriceRange.EXPENSIVE);
        
        Restaurant r3 = new Restaurant();
        r3.setName("Bella Italia");
        r3.setCuisine("Italian");
        r3.setDescription("Traditional Italian pasta and pizza");
        r3.setImage("https://images.unsplash.com/photo-1555396273-367ea4eb4db5");
        r3.setAddress("789 Pine Rd, Little Italy");
        r3.setPhone("+1-555-0103");
        r3.setSeatingCapacity(50);
        r3.setOpeningHours("11:30 AM - 10:30 PM");
        r3.setRating(4.3);
        r3.setReviewCount(0);
        r3.setIsOpen(true);
        r3.setFeatures(Arrays.asList("Family Friendly", "Pizza Oven", "Wine List"));
        r3.setPriceRange(Restaurant.PriceRange.MODERATE);
        
        List<Restaurant> restaurants = Arrays.asList(r1, r2, r3);
        return restaurantRepository.saveAll(restaurants);
    }
    
    private void seedMenuItems(List<Restaurant> restaurants) {
        List<MenuItem> items = new ArrayList<>();
        
        // Menu for Le Gourmet
        MenuItem m1 = new MenuItem();
        m1.setRestaurantId(restaurants.get(0).getId());
        m1.setName("Foie Gras");
        m1.setDescription("Pan-seared foie gras with fig compote");
        m1.setImage("https://images.unsplash.com/photo-1544025162-d76694265947");
        m1.setPrice(28.00);
        m1.setCategory(MenuItem.MenuCategory.STARTERS);
        items.add(m1);
        
        MenuItem m2 = new MenuItem();
        m2.setRestaurantId(restaurants.get(0).getId());
        m2.setName("Coq au Vin");
        m2.setDescription("Braised chicken in red wine sauce");
        m2.setImage("https://images.unsplash.com/photo-1598103442097-8b74394b95c6");
        m2.setPrice(42.00);
        m2.setCategory(MenuItem.MenuCategory.MAIN_COURSES);
        items.add(m2);
        
        // Menu for Sushi Paradise
        MenuItem m3 = new MenuItem();
        m3.setRestaurantId(restaurants.get(1).getId());
        m3.setName("Sashimi Platter");
        m3.setDescription("Assorted fresh sashimi");
        m3.setImage("https://images.unsplash.com/photo-1579584425555-c3ce17fd4351");
        m3.setPrice(35.00);
        m3.setCategory(MenuItem.MenuCategory.MAIN_COURSES);
        items.add(m3);
        
        menuItemRepository.saveAll(items);
    }
    
    private void seedReviews(List<User> users, List<Restaurant> restaurants) {
        List<Review> reviews = new ArrayList<>();
        
        Review r1 = new Review();
        r1.setUserId(users.get(1).getId());
        r1.setRestaurantId(restaurants.get(0).getId());
        r1.setRating(5.0);
        r1.setTitle("Exceptional French cuisine");
        r1.setComment("The coq au vin was absolutely divine. Best French restaurant in the city!");
        r1.setHelpfulCount(12);
        r1.setStatus(Review.ReviewStatus.PUBLISHED);
        reviews.add(r1);
        
        Review r2 = new Review();
        r2.setUserId(users.get(2).getId());
        r2.setRestaurantId(restaurants.get(1).getId());
        r2.setRating(5.0);
        r2.setTitle("Best sushi in town");
        r2.setComment("Fresh fish, excellent presentation. The chef is a true master!");
        r2.setHelpfulCount(8);
        r2.setStatus(Review.ReviewStatus.PUBLISHED);
        reviews.add(r2);
        
        reviewRepository.saveAll(reviews);
    }
    
    private void seedEvents(List<Restaurant> restaurants) {
        List<Event> events = new ArrayList<>();
        
        Event e1 = new Event();
        e1.setRestaurantId(restaurants.get(0).getId());
        e1.setTitle("Wine Tasting Evening");
        e1.setDescription("Exclusive wine tasting with our sommelier");
        e1.setImage("https://images.unsplash.com/photo-1510812431401-41d2bd2722f3");
        e1.setEventDate(LocalDateTime.now().plusDays(7));
        e1.setPrice(75.00);
        e1.setAvailableSpots(20);
        e1.setBookedSpots(5);
        e1.setCategory(Event.EventCategory.WINE_TASTING);
        events.add(e1);
        
        eventRepository.saveAll(events);
    }
    
    private void seedHangOuts(List<User> users, List<Restaurant> restaurants) {
        List<HangOut> hangouts = new ArrayList<>();
        
        HangOut h1 = new HangOut();
        h1.setCreatorId(users.get(0).getId());
        h1.setRestaurantId(restaurants.get(0).getId());
        h1.setTitle("Friday Night Dinner");
        h1.setDescription("Let's try the new French restaurant!");
        h1.setPlannedDateTime(LocalDateTime.now().plusDays(5));
        h1.setMaxParticipants(6);
        h1.setStatus(HangOut.HangOutStatus.OPEN);
        
        List<HangOut.Participant> participants = new ArrayList<>();
        HangOut.Participant p1 = new HangOut.Participant();
        p1.setUserId(users.get(1).getId());
        p1.setStatus(HangOut.ParticipantStatus.ACCEPTED);
        participants.add(p1);
        h1.setParticipants(participants);
        
        hangouts.add(h1);
        hangOutRepository.saveAll(hangouts);
    }

    private void seedQuizQuestions() {
        List<QuizQuestion> questions = new ArrayList<>();

        questions.add(quizQuestion(
            "A quelle temperature ideale sert-on generalement un vin rouge leger ?",
            List.of("4-6°C", "12-14°C", "18-20°C", "25-28°C"), 1,
            "Un vin rouge leger (comme un Beaujolais) se sert legerement frais, autour de 12-14°C, pour preserver ses aromes fruites.",
            "Vin", QuizQuestion.Difficulty.MEDIUM));

        questions.add(quizQuestion(
            "Quel ingredient est la base d'un risotto traditionnel ?",
            List.of("Riz Arborio", "Pates", "Semoule", "Quinoa"), 0,
            "Le risotto italien se prepare traditionnellement avec du riz Arborio, riche en amidon, pour obtenir sa texture cremeuse.",
            "Cuisine", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Combien de points gagne-t-on generalement en laissant un avis sur un restaurant ?",
            List.of("Cela depend du programme", "0 point", "1000 points", "Un avis ne rapporte jamais de points"), 0,
            "Les points accordes pour un avis dependent des regles du programme de fidelite en vigueur dans l'application.",
            "Programme fidelite", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Quelle cuisine est reputee pour le sushi et le ramen ?",
            List.of("Italienne", "Japonaise", "Mexicaine", "Indienne"), 1,
            "Le sushi et le ramen sont des plats embematiques de la cuisine japonaise.",
            "Cuisine", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Dans un menu, que designe generalement le terme 'entree' en France ?",
            List.of("Le plat principal", "Le dessert", "Le premier service avant le plat principal", "La boisson d'accueil"), 2,
            "En France, l'entree est le service qui precede le plat principal, contrairement aux Etats-Unis ou 'entree' designe le plat principal.",
            "Culture culinaire", QuizQuestion.Difficulty.MEDIUM));

        questions.add(quizQuestion(
            "Quel est l'ingredient principal du guacamole ?",
            List.of("Tomate", "Avocat", "Poivron", "Mais"), 1,
            "Le guacamole est une preparation mexicaine a base d'avocat ecrase.",
            "Cuisine", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Qu'est-ce qu'un 'amuse-bouche' au restaurant ?",
            List.of("Une petite bouchee offerte avant le repas", "Le plat principal", "Un digestif", "Le pain de la table"), 0,
            "L'amuse-bouche est une petite bouchee, generalement offerte par le chef, servie avant le debut du repas.",
            "Culture culinaire", QuizQuestion.Difficulty.MEDIUM));

        questions.add(quizQuestion(
            "Quel fromage est traditionnellement utilise sur une pizza margherita ?",
            List.of("Roquefort", "Mozzarella", "Comte", "Feta"), 1,
            "La pizza margherita traditionnelle utilise de la mozzarella, en reference aux couleurs du drapeau italien avec la tomate et le basilic.",
            "Cuisine", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Combien de tentatives gratuites a-t-on generalement par jour pour la roue quotidienne ?",
            List.of("1", "5", "10", "Illimite"), 0,
            "La roue quotidienne de fidelite offre generalement 1 tour gratuit par jour.",
            "Programme fidelite", QuizQuestion.Difficulty.EASY));

        questions.add(quizQuestion(
            "Quelle epice donne sa couleur jaune caracteristique au curry ?",
            List.of("Paprika", "Curcuma", "Cannelle", "Safran"), 1,
            "Le curcuma est l'epice responsable de la couleur jaune vif de nombreux curry.",
            "Cuisine", QuizQuestion.Difficulty.MEDIUM));

        quizQuestionRepository.saveAll(questions);
    }

    private QuizQuestion quizQuestion(String question, List<String> options, int correctIndex, String explanation, String category, QuizQuestion.Difficulty difficulty) {
        QuizQuestion q = new QuizQuestion();
        q.setQuestion(question);
        q.setOptions(options);
        q.setCorrectAnswerIndex(correctIndex);
        q.setExplanation(explanation);
        q.setCategory(category);
        q.setDifficulty(difficulty);
        q.setActive(true);
        return q;
    }

    private void seedRewards() {
        List<Reward> rewards = new ArrayList<>();

        rewards.add(reward("Credit de 10$", "10$ de reduction sur ta prochaine reservation", Reward.RewardCategory.DISCOUNT, 500, null,
            "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=400", "Valable sur une seule reservation, non cumulable."));

        rewards.add(reward("Entree offerte", "Une entree offerte dans les restaurants partenaires", Reward.RewardCategory.FREE_ITEM, 300, 50,
            "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400", "Valable 30 jours apres l'echange, une seule entree par reservation."));

        rewards.add(reward("Credit de 25$", "25$ de reduction sur ta prochaine reservation", Reward.RewardCategory.DISCOUNT, 1000, null,
            "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400", "Valable sur une seule reservation, non cumulable."));

        rewards.add(reward("Dessert offert", "Un dessert offert pour toute la table", Reward.RewardCategory.FREE_ITEM, 450, 30,
            "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400", "Valable pour une table de maximum 4 personnes."));

        rewards.add(reward("Experience degustation", "Acces a une degustation exclusive avec un chef partenaire", Reward.RewardCategory.EXPERIENCE, 2000, 10,
            "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400", "Sur reservation uniquement, places limitees."));

        rewards.add(reward("Statut VIP 1 mois", "Acces prioritaire aux reservations pendant 1 mois", Reward.RewardCategory.TIER_PERK, 1500, null,
            "https://images.unsplash.com/photo-1529636798458-92182e662485?w=400", "Avantage valable 30 jours a compter de l'echange."));

        rewards.add(reward("Credit de 50$", "50$ de reduction sur ta prochaine reservation", Reward.RewardCategory.DISCOUNT, 1800, null,
            "https://images.unsplash.com/photo-1424847651672-bf20a4b0982b?w=400", "Valable sur une seule reservation, non cumulable."));

        rewards.add(reward("Diner pour 2 offert", "Un diner complet offert pour deux personnes", Reward.RewardCategory.EXPERIENCE, 3500, 5,
            "https://images.unsplash.com/photo-1424847651672-bf20a4b0982b?w=400", "Sur reservation, hors boissons alcoolisees."));

        rewardRepository.saveAll(rewards);
    }

    private void seedRestaurantRewards() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            return;
        }
        List<Reward> rewards = new ArrayList<>();
        LocalDateTime inTwoMonths = LocalDateTime.now().plusMonths(2);

        for (Restaurant r : restaurants) {
            Reward starter = restaurantReward("Entree offerte chez " + r.getName(),
                "Une entree offerte pour ta prochaine visite chez " + r.getName() + ".",
                Reward.RewardCategory.FREE_ITEM, 250, 20, r.getImage(), r.getId(),
                "Valable pour une reservation chez " + r.getName() + " uniquement.", inTwoMonths);
            Reward premium = restaurantReward("Menu degustation chez " + r.getName(),
                "Un menu degustation complet pour deux personnes chez " + r.getName() + ".",
                Reward.RewardCategory.EXPERIENCE, 1200, 8, r.getImage(), r.getId(),
                "Sur reservation uniquement chez " + r.getName() + ", places limitees.", inTwoMonths);
            rewards.add(starter);
            rewards.add(premium);
        }

        rewardRepository.saveAll(rewards);
    }

    private Reward restaurantReward(String name, String description, Reward.RewardCategory category, int pointsCost,
                                     Integer stock, String imageUrl, String restaurantId, String terms, LocalDateTime expiry) {
        Reward r = reward(name, description, category, pointsCost, stock, imageUrl, terms);
        r.setRestaurantId(restaurantId);
        r.setExpiryDate(expiry);
        return r;
    }

    private Reward reward(String name, String description, Reward.RewardCategory category, int pointsCost, Integer stock, String imageUrl, String terms) {
        Reward r = new Reward();
        r.setName(name);
        r.setDescription(description);
        r.setCategory(category);
        r.setPointsCost(pointsCost);
        r.setStock(stock);
        r.setActive(true);
        r.setImageUrl(imageUrl);
        r.setTermsAndConditions(terms);
        return r;
    }
}
