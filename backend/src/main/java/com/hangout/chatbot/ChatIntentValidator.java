package com.hangout.chatbot;

import com.hangout.config.ReservationProperties;
import com.hangout.model.Event;
import com.hangout.model.Restaurant;
import com.hangout.repository.EventRepository;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Second protection layer: checks every value extracted by the model before any business operation.
 * Restaurants are matched against the database, dates and party sizes against the business rules.
 */
@Component
@RequiredArgsConstructor
public class ChatIntentValidator {
    
    private static final int MAX_DAYS_AHEAD = 365;
    private static final int MAX_SPECIAL_REQUESTS_LENGTH = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    
    private final RestaurantRepository restaurantRepository;
    private final EventRepository eventRepository;
    private final ReservationProperties reservationProperties;
    private final InputGuard inputGuard;
    private final Clock clock;

    public ValidatedRequest validate(ExtractedRequest extracted, String contextRestaurantId) {
        return validate(extracted, contextRestaurantId, null);
    }

    public ValidatedRequest validate(ExtractedRequest extracted, String contextRestaurantId, String contextEventId) {
        List<String> problems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        
        // Restaurant: must exist in the database
        Restaurant restaurant = null;
        boolean restaurantNamed = notBlank(extracted.restaurantName());
        if (restaurantNamed) {
            List<Restaurant> all = restaurantRepository.findAll();
            List<Restaurant> matches = matchRestaurants(extracted.restaurantName(), all);
            if (matches.size() == 1) {
                restaurant = matches.get(0);
            } else if (matches.isEmpty()) {
                problems.add("Je ne trouve pas le restaurant « " + inputGuard.sanitize(extracted.restaurantName())
                    + " ». Nos restaurants : " + names(all) + ". Lequel souhaitez-vous ?");
            } else {
                problems.add("Plusieurs restaurants correspondent : " + names(matches) + ". Lequel souhaitez-vous ?");
            }
        } else if (notBlank(contextRestaurantId)) {
            restaurant = restaurantRepository.findById(contextRestaurantId).orElse(null);
        }

        // Event: must exist in the database
        Event event = null;
        boolean eventNamed = notBlank(extracted.eventTitle());
        if (eventNamed) {
            List<Event> all = eventRepository.findAll();
            List<Event> matches = matchEvents(extracted.eventTitle(), all);
            if (matches.size() == 1) {
                event = matches.get(0);
            } else if (matches.isEmpty()) {
                problems.add("Je ne trouve pas l'événement « " + inputGuard.sanitize(extracted.eventTitle())
                    + " ». Événements à venir : " + eventTitles(all) + ". Lequel souhaitez-vous ?");
            } else {
                problems.add("Plusieurs événements correspondent : " + eventTitles(matches) + ". Lequel souhaitez-vous ?");
            }
        } else if (notBlank(contextEventId)) {
            event = eventRepository.findById(contextEventId).orElse(null);
        }

        // Date
        LocalDate date = null;
        if (notBlank(extracted.date())) {
            Optional<LocalDate> parsed = parseDate(extracted.date());
            if (parsed.isEmpty()) {
                problems.add("Je n'ai pas compris la date. Pouvez-vous la préciser (par exemple « samedi 18 octobre ») ?");
            } else if (parsed.get().isBefore(today)) {
                problems.add("La date du " + ChatFormatter.date(parsed.get()) + " est déjà passée. Pour quelle date souhaitez-vous réserver ?");
            } else if (parsed.get().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
                problems.add("Les réservations sont possibles jusqu'à un an à l'avance. Pour quelle date souhaitez-vous réserver ?");
            } else {
                date = parsed.get();
            }
        }
        
        // Time
        LocalTime time = null;
        if (notBlank(extracted.time())) {
            Optional<LocalTime> parsed = parseTime(extracted.time());
            if (parsed.isEmpty()) {
                problems.add("Je n'ai pas compris l'heure. À quelle heure souhaitez-vous venir (par exemple 20h00) ?");
            } else if (date != null && !date.atTime(parsed.get()).isAfter(now)) {
                problems.add("Cette heure est déjà passée. À quelle heure souhaitez-vous venir ?");
            } else {
                time = parsed.get();
            }
        }
        
        // Party size: same bounds as the reservation form
        Integer partySize = extracted.partySize();
        if (partySize != null && (partySize < reservationProperties.getMinPartySize()
                || partySize > reservationProperties.getMaxPartySize())) {
            problems.add("Le nombre de personnes doit être compris entre " + reservationProperties.getMinPartySize()
                + " et " + reservationProperties.getMaxPartySize() + ". Pour combien de personnes souhaitez-vous réserver ?");
            partySize = null;
        }
        
        Integer reservationNumber = extracted.reservationNumber();
        if (reservationNumber != null && reservationNumber < 1) {
            reservationNumber = null;
        }
        
        LocalDate targetDate = notBlank(extracted.targetDate()) ? parseDate(extracted.targetDate()).orElse(null) : null;
        
        String specialRequests = null;
        if (notBlank(extracted.specialRequests())) {
            String cleaned = inputGuard.sanitize(extracted.specialRequests());
            if (!inputGuard.looksLikeInjection(cleaned)) {
                specialRequests = cleaned.length() > MAX_SPECIAL_REQUESTS_LENGTH
                    ? cleaned.substring(0, MAX_SPECIAL_REQUESTS_LENGTH) : cleaned;
            }
        }
        
        return new ValidatedRequest(extracted.chatIntent(), restaurant, restaurantNamed && restaurant != null,
            date, time, partySize, reservationNumber, targetDate, specialRequests,
            InfoTopic.fromModel(extracted.infoTopic()), event, eventNamed && event != null, List.copyOf(problems));
    }

    List<Event> matchEvents(String requested, List<Event> all) {
        String wanted = key(requested);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<Event> exact = all.stream().filter(e -> key(e.getTitle()).equals(wanted)).toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return all.stream()
            .filter(e -> {
                String title = key(e.getTitle());
                return !title.isEmpty() && (title.contains(wanted) || wanted.contains(title));
            })
            .toList();
    }

    private static String eventTitles(List<Event> events) {
        return events.stream().map(Event::getTitle).collect(Collectors.joining(", "));
    }

    List<Restaurant> matchRestaurants(String requested, List<Restaurant> all) {
        String wanted = key(requested);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<Restaurant> exact = all.stream().filter(r -> key(r.getName()).equals(wanted)).toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return all.stream()
            .filter(r -> {
                String name = key(r.getName());
                return !name.isEmpty() && (name.contains(wanted) || wanted.contains(name));
            })
            .toList();
    }
    
    // "Le Gourmet", "au gourmet", "chez Le Gourmet" -> "gourmet"
    private static String key(String name) {
        if (name == null) {
            return "";
        }
        String s = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9 ]", " ");
        s = (" " + s + " ").replaceAll(" (le|la|les|l|au|aux|chez|restaurant|du|de|des) ", " ");
        s = s.replaceAll(" (le|la|les|l|au|aux|chez|restaurant|du|de|des) ", " ");
        return s.replaceAll("\\s+", "");
    }
    
    private static String names(List<Restaurant> restaurants) {
        return restaurants.stream().map(Restaurant::getName).collect(Collectors.joining(", "));
    }
    
    private static Optional<LocalDate> parseDate(String value) {
        try {
            return Optional.of(LocalDate.parse(value.trim()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    private static Optional<LocalTime> parseTime(String value) {
        try {
            return Optional.of(LocalTime.parse(value.trim(), TIME_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
    
    private static boolean notBlank(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim());
    }
}
