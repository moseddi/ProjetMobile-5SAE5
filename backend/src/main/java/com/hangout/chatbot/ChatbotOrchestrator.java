package com.hangout.chatbot;

import com.hangout.config.ReservationProperties;
import com.hangout.dto.AvailabilityResult;
import com.hangout.dto.chat.ChatConfirmRequest;
import com.hangout.dto.chat.ChatMessageRequest;
import com.hangout.dto.chat.ChatResponse;
import com.hangout.dto.chat.ChatResponse.Type;
import com.hangout.dto.chat.ChatTurn;
import com.hangout.dto.chat.EventBookingProposal;
import com.hangout.dto.chat.EventBookingSummary;
import com.hangout.dto.chat.EventChatConfirmRequest;
import com.hangout.dto.chat.EventProposalAction;
import com.hangout.dto.chat.ProposalAction;
import com.hangout.dto.chat.ReservationProposal;
import com.hangout.dto.chat.ReservationSummary;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ConflictException;
import com.hangout.model.Event;
import com.hangout.model.EventBooking;
import com.hangout.model.Reservation;
import com.hangout.model.Restaurant;
import com.hangout.repository.EventRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.service.AvailabilityService;
import com.hangout.service.EventBookingService;
import com.hangout.service.ReservationService;
import com.hangout.util.OpeningHoursParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hangout.chatbot.ChatFormatter.dateTime;
import static com.hangout.chatbot.ChatFormatter.persons;

/**
 * Pipeline of the reservation assistant:
 * 1. InputGuard (size, injection, rate limit)  2. GroqClient (classification + extraction only)
 * 3. ChatIntentValidator (checks every value)   4. AvailabilityService / ReservationService (real data)
 * 5. Reply sentences built here from what the database returned. The model never writes the reply,
 *    so it cannot invent an availability or a confirmation.
 * Writes happen only in confirm(), after an explicit click of the user, through ReservationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotOrchestrator {
    
    public static final String OUT_OF_SCOPE_REPLY = "Je suis l’assistant intelligent du système de réservation de "
        + "HangOut. Je peux uniquement vous aider à réserver une table, consulter les disponibilités, réserver un "
        + "événement, ou modifier/annuler une réservation. Comment puis-je vous aider ?";
    
    static final String NOT_UNDERSTOOD_REPLY = "Je n'ai pas bien compris votre demande. Pouvez-vous la reformuler ? "
        + "Par exemple : « Je veux réserver une table pour 2 personnes demain à 20h ».";
    
    private static final int MAX_HISTORY_CONTENT_LENGTH = 1000;
    
    private final InputGuard inputGuard;
    private final GroqClient groqClient;
    private final PromptFactory promptFactory;
    private final ChatIntentValidator validator;
    private final AvailabilityService availabilityService;
    private final ReservationService reservationService;
    private final RestaurantRepository restaurantRepository;
    private final ReservationProperties reservationProperties;
    private final ChatbotProperties chatbotProperties;
    private final Clock clock;
    private final EventRepository eventRepository;
    private final EventBookingService eventBookingService;
    
    public ChatResponse handleMessage(String userId, ChatMessageRequest request) {
        switch (inputGuard.inspect(request.message())) {
            case EMPTY:
                return ChatResponse.of("Comment puis-je vous aider pour votre réservation ?", null, Type.QUESTION);
            case TOO_LONG:
                return ChatResponse.of("Votre message est trop long (" + chatbotProperties.getMaxMessageLength()
                    + " caractères maximum). Pouvez-vous le raccourcir ?", null, Type.QUESTION);
            case INJECTION:
                log.info("Chatbot: tentative d'injection bloquée avant l'appel au modèle");
                return outOfScope();
            default:
                break;
        }
        inputGuard.checkRateLimit(userId);
        
        ExtractedRequest extracted;
        try {
            extracted = groqClient.extract(buildMessages(request));
        } catch (AiServiceException e) {
            if (e.getType() == AiServiceException.Type.INVALID_RESPONSE) {
                log.warn("Chatbot: réponse du modèle inexploitable ({})", e.getMessage());
                return ChatResponse.of(NOT_UNDERSTOOD_REPLY, null, Type.QUESTION);
            }
            throw e;
        }
        
        ValidatedRequest validated = validator.validate(extracted, request.restaurantId(), request.eventId());
        log.debug("Chatbot: intent={} problems={}", validated.intent(), validated.problems().size());

        ChatResponse response = switch (validated.intent()) {
            case CREATE, CHECK_AVAILABILITY -> handleCreateOrCheck(validated);
            case MODIFY -> handleModify(userId, validated);
            case CANCEL -> handleCancel(userId, validated);
            case STATUS -> handleStatus(userId, validated);
            case INFO -> handleInfo(validated);
            case BOOK_EVENT, CHECK_EVENTS -> handleBookOrCheckEvent(validated);
            case CANCEL_EVENT_BOOKING -> handleCancelEventBooking(userId, validated);
            case EVENT_STATUS -> handleEventStatus(userId, validated);
            case OUT_OF_SCOPE -> outOfScope();
        };
        if (response.restaurantId() == null && validated.restaurant() != null) {
            response = response.withRestaurant(validated.restaurant().getId());
        }
        if (response.eventId() == null && validated.event() != null) {
            response = response.withEvent(validated.event().getId());
        }
        return response;
    }
    
    List<Map<String, String>> buildMessages(ChatMessageRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", promptFactory.systemPrompt(LocalDate.now(clock))));
        List<ChatTurn> history = inputGuard.cleanHistory(request.historyOrEmpty());
        int from = Math.max(0, history.size() - chatbotProperties.getMaxHistory());
        for (ChatTurn turn : history.subList(from, history.size())) {
            String content = inputGuard.sanitize(turn.content());
            if (content.length() > MAX_HISTORY_CONTENT_LENGTH) {
                content = content.substring(0, MAX_HISTORY_CONTENT_LENGTH);
            }
            messages.add(Map.of("role", turn.role(), "content", content));
        }
        messages.add(Map.of("role", "user", "content", inputGuard.sanitize(request.message())));
        return messages;
    }
    
    // ---------------------------------------------------------------- creation / availability
    
    private ChatResponse handleCreateOrCheck(ValidatedRequest r) {
        ChatIntent intent = r.intent();
        if (r.hasProblems()) {
            return ChatResponse.of(r.problems().get(0), intent, Type.QUESTION);
        }
        Restaurant restaurant = r.restaurant();
        if (restaurant == null) {
            return ChatResponse.of("Dans quel restaurant souhaitez-vous réserver ? Nos restaurants : "
                + restaurantNames() + ".", intent, Type.QUESTION);
        }
        if (r.date() == null) {
            return ChatResponse.of("Pour quelle date souhaitez-vous réserver chez " + restaurant.getName() + " ?",
                intent, Type.QUESTION);
        }
        if (r.partySize() == null) {
            return ChatResponse.of("Pour combien de personnes souhaitez-vous réserver ?", intent, Type.QUESTION);
        }
        if (r.time() == null) {
            List<LocalDateTime> slots = availabilityService.availableSlots(restaurant.getId(), r.date(), r.partySize(), null);
            if (slots.isEmpty()) {
                return ChatResponse.of("Désolé, il n'y a plus de créneau disponible chez " + restaurant.getName()
                    + " le " + ChatFormatter.date(r.date()) + " pour " + persons(r.partySize())
                    + ". Souhaitez-vous essayer une autre date ?", intent, Type.ANSWER);
            }
            List<LocalDateTime> shown = slots.stream().limit(chatbotProperties.getMaxSuggestedSlots()).toList();
            return ChatResponse.of("Voici les créneaux disponibles chez " + restaurant.getName() + " le "
                    + ChatFormatter.date(r.date()) + " pour " + persons(r.partySize()) + " : "
                    + ChatFormatter.slots(shown) + (slots.size() > shown.size() ? "…" : "")
                    + ". À quelle heure souhaitez-vous venir ?", intent, Type.QUESTION)
                .withSlots(shown);
        }
        
        LocalDateTime requested = r.date().atTime(r.time());
        AvailabilityResult result = availabilityService.checkAvailability(restaurant.getId(), requested, r.partySize());
        if (!result.available()) {
            return unavailable(result, restaurant, intent);
        }
        String details = restaurant.getName() + " le " + dateTime(requested) + " pour " + persons(r.partySize());
        String specials = r.specialRequests() != null ? " Demande particulière : « " + r.specialRequests() + " »." : "";
        String reply = intent == ChatIntent.CHECK_AVAILABILITY
            ? "Oui, il reste de la place chez " + details + "." + specials + " Souhaitez-vous réserver ce créneau ?"
            : "Une table est disponible chez " + details + "." + specials + " Confirmez-vous la réservation ?";
        return ChatResponse.of(reply, intent, Type.PROPOSAL)
            .withProposal(new ReservationProposal(ProposalAction.CREATE, null, restaurant.getId(), restaurant.getName(),
                requested, r.partySize(), r.specialRequests()));
    }
    
    private ChatResponse unavailable(AvailabilityResult result, Restaurant restaurant, ChatIntent intent) {
        String reply = switch (result.reason()) {
            case RESTAURANT_CLOSED -> restaurant.getName() + " n'accepte pas de réservation pour le moment.";
            case PARTY_TOO_LARGE_FOR_RESTAURANT -> restaurant.getName() + " ne peut pas accueillir "
                + persons(result.partySize()) + " (capacité maximale : " + availabilityService.capacityOf(restaurant) + ").";
            case OUTSIDE_OPENING_HOURS -> "Ce créneau est en dehors des horaires de réservation de " + restaurant.getName()
                + " (" + restaurant.getOpeningHours() + ", dernière réservation "
                + reservationProperties.getLastSeatingMinutesBeforeClose() + " min avant la fermeture).";
            default -> "Désolé, il n'y a plus assez de places chez " + restaurant.getName() + " le "
                + dateTime(result.requestedDateTime()) + " pour " + persons(result.partySize()) + ".";
        };
        if (result.alternatives().isEmpty()) {
            return ChatResponse.of(reply + " Souhaitez-vous essayer une autre date ?", intent, Type.ANSWER);
        }
        return ChatResponse.of(reply + " Créneaux disponibles les plus proches : "
                + ChatFormatter.slots(result.alternatives()) + ". Lequel vous convient ?", intent, Type.QUESTION)
            .withSlots(result.alternatives());
    }
    
    // ---------------------------------------------------------------- modification / cancellation
    
    private ChatResponse handleModify(String userId, ValidatedRequest r) {
        List<Reservation> upcoming = upcomingReservations(userId);
        if (upcoming.isEmpty()) {
            return ChatResponse.of("Vous n'avez aucune réservation à venir à modifier.", ChatIntent.MODIFY, Type.ANSWER);
        }
        Map<String, Restaurant> restaurants = restaurantsById();
        Reservation target = resolveTarget(upcoming, r);
        if (target == null) {
            return ChatResponse.of("Vous avez plusieurs réservations à venir. Laquelle souhaitez-vous modifier ? "
                    + "Indiquez son numéro.", ChatIntent.MODIFY, Type.QUESTION)
                .withReservations(summaries(upcoming, restaurants));
        }
        if (r.hasProblems()) {
            return ChatResponse.of(r.problems().get(0), ChatIntent.MODIFY, Type.QUESTION);
        }
        Restaurant current = restaurants.get(target.getRestaurantId());
        String currentName = current != null ? current.getName() : "ce restaurant";
        boolean changesRestaurant = r.restaurantNamed() && !r.restaurant().getId().equals(target.getRestaurantId());
        if (r.date() == null && r.time() == null && r.partySize() == null && !changesRestaurant && r.specialRequests() == null) {
            return ChatResponse.of("Que souhaitez-vous modifier pour votre réservation chez " + currentName + " le "
                    + dateTime(target.getReservationDateTime()) + " : la date, l'heure ou le nombre de personnes ?",
                    ChatIntent.MODIFY, Type.QUESTION)
                .withReservations(summaries(List.of(target), restaurants, upcoming));
        }
        
        Restaurant newRestaurant = changesRestaurant ? r.restaurant() : current;
        if (newRestaurant == null) {
            return ChatResponse.of("Le restaurant de cette réservation est introuvable.", ChatIntent.MODIFY, Type.ANSWER);
        }
        LocalDate newDate = r.date() != null ? r.date() : target.getReservationDateTime().toLocalDate();
        LocalTime newTime = r.time() != null ? r.time() : target.getReservationDateTime().toLocalTime();
        LocalDateTime newDateTime = newDate.atTime(newTime);
        int newPartySize = r.partySize() != null ? r.partySize() : target.getPartySize();
        String newSpecials = r.specialRequests() != null ? r.specialRequests() : target.getSpecialRequests();
        if (!newDateTime.isAfter(LocalDateTime.now(clock))) {
            return ChatResponse.of("Ce nouveau créneau est déjà passé. Quelle date et quelle heure souhaitez-vous ?",
                ChatIntent.MODIFY, Type.QUESTION);
        }
        
        AvailabilityResult result = availabilityService.checkAvailability(newRestaurant.getId(), newDateTime,
            newPartySize, target.getId());
        if (!result.available()) {
            return unavailable(result, newRestaurant, ChatIntent.MODIFY);
        }
        String reply = "Je peux modifier votre réservation. Avant : " + currentName + ", "
            + dateTime(target.getReservationDateTime()) + ", " + persons(target.getPartySize()) + ". Après : "
            + newRestaurant.getName() + ", " + dateTime(newDateTime) + ", " + persons(newPartySize)
            + ". Confirmez-vous la modification ?";
        return ChatResponse.of(reply, ChatIntent.MODIFY, Type.PROPOSAL)
            .withProposal(new ReservationProposal(ProposalAction.MODIFY, target.getId(), newRestaurant.getId(),
                newRestaurant.getName(), newDateTime, newPartySize, newSpecials));
    }
    
    private ChatResponse handleCancel(String userId, ValidatedRequest r) {
        List<Reservation> upcoming = upcomingReservations(userId);
        if (upcoming.isEmpty()) {
            return ChatResponse.of("Vous n'avez aucune réservation à venir à annuler.", ChatIntent.CANCEL, Type.ANSWER);
        }
        Map<String, Restaurant> restaurants = restaurantsById();
        Reservation target = resolveTarget(upcoming, r);
        if (target == null) {
            return ChatResponse.of("Vous avez plusieurs réservations à venir. Laquelle souhaitez-vous annuler ? "
                    + "Indiquez son numéro.", ChatIntent.CANCEL, Type.QUESTION)
                .withReservations(summaries(upcoming, restaurants));
        }
        Restaurant restaurant = restaurants.get(target.getRestaurantId());
        String name = restaurant != null ? restaurant.getName() : "ce restaurant";
        return ChatResponse.of("Voulez-vous vraiment annuler votre réservation chez " + name + " le "
                + dateTime(target.getReservationDateTime()) + " pour " + persons(target.getPartySize()) + " ?",
                ChatIntent.CANCEL, Type.PROPOSAL)
            .withProposal(new ReservationProposal(ProposalAction.CANCEL, target.getId(), target.getRestaurantId(), name,
                target.getReservationDateTime(), target.getPartySize(), target.getSpecialRequests()));
    }
    
    /**
     * Picks the reservation the user refers to: by number in the list shown earlier, by date, or the only one.
     * Returns null when it is ambiguous, so the assistant asks instead of guessing.
     */
    Reservation resolveTarget(List<Reservation> upcoming, ValidatedRequest r) {
        if (r.reservationNumber() != null && r.reservationNumber() <= upcoming.size()) {
            return upcoming.get(r.reservationNumber() - 1);
        }
        if (r.targetDate() != null) {
            List<Reservation> sameDay = upcoming.stream()
                .filter(res -> res.getReservationDateTime().toLocalDate().equals(r.targetDate()))
                .toList();
            if (sameDay.size() == 1) {
                return sameDay.get(0);
            }
        }
        return upcoming.size() == 1 ? upcoming.get(0) : null;
    }
    
    // ---------------------------------------------------------------- status / info
    
    private ChatResponse handleStatus(String userId, ValidatedRequest r) {
        List<Reservation> upcoming = upcomingReservations(userId);
        Map<String, Restaurant> restaurants = restaurantsById();
        if (upcoming.isEmpty()) {
            return ChatResponse.of("Vous n'avez aucune réservation à venir. Souhaitez-vous réserver une table ?",
                ChatIntent.STATUS, Type.ANSWER);
        }
        if (r.reservationNumber() != null || r.targetDate() != null) {
            Reservation target = resolveTarget(upcoming, r);
            if (target != null) {
                Restaurant restaurant = restaurants.get(target.getRestaurantId());
                return ChatResponse.of("Votre réservation chez " + (restaurant != null ? restaurant.getName() : "ce restaurant")
                        + " le " + dateTime(target.getReservationDateTime()) + " pour " + persons(target.getPartySize())
                        + " est " + ChatFormatter.status(target.getStatus()) + ".", ChatIntent.STATUS, Type.ANSWER)
                    .withReservations(summaries(List.of(target), restaurants, upcoming));
            }
        }
        String reply = upcoming.size() == 1
            ? "Vous avez 1 réservation à venir :"
            : "Vous avez " + upcoming.size() + " réservations à venir :";
        return ChatResponse.of(reply, ChatIntent.STATUS, Type.ANSWER).withReservations(summaries(upcoming, restaurants));
    }
    
    private ChatResponse handleInfo(ValidatedRequest r) {
        String reply = switch (r.infoTopic()) {
            case REQUIRED_INFO -> "Pour réserver une table, il me faut le restaurant, la date, l'heure et le nombre de "
                + "personnes (de " + reservationProperties.getMinPartySize() + " à " + reservationProperties.getMaxPartySize()
                + "). Vous pouvez ajouter une demande particulière (terrasse, chaise haute, allergie…). "
                + "Exemple : « Une table pour 2 demain à 20h chez Bella Italia ».";
            case HOW_TO_CANCEL -> "Pour annuler, écrivez-moi « annuler ma réservation » : je vous montrerai la réservation "
                + "concernée et vous demanderai confirmation. Vous pouvez aussi cliquer sur « Cancel Reservation » dans la "
                + "liste de vos réservations.";
            case HOW_TO_MODIFY -> "Pour modifier une réservation, dites-moi ce que vous voulez changer, par exemple "
                + "« décaler ma réservation de samedi à 21h » ou « passer à 4 personnes ». Je vérifierai la disponibilité "
                + "avant de vous demander confirmation.";
            case OPENING_HOURS -> openingHoursInfo(r.restaurant());
            case GENERAL -> "Je peux vous aider à réserver une table, vérifier les disponibilités, modifier ou annuler "
                + "une réservation, et consulter vos réservations. Que souhaitez-vous faire ?";
        };
        return ChatResponse.of(reply, ChatIntent.INFO, Type.ANSWER);
    }
    
    private String openingHoursInfo(Restaurant restaurant) {
        String rule = " Les réservations se font par créneaux de " + reservationProperties.getSlotDurationMinutes() / 60
            + " h, jusqu'à " + reservationProperties.getLastSeatingMinutesBeforeClose() + " min avant la fermeture.";
        if (restaurant != null) {
            return "Horaires de " + restaurant.getName() + " : " + displayHours(restaurant) + "." + rule;
        }
        return "Horaires : " + restaurantRepository.findAll().stream()
            .map(r -> r.getName() + " (" + displayHours(r) + ")")
            .collect(Collectors.joining(", ")) + "." + rule;
    }
    
    private static String displayHours(Restaurant restaurant) {
        return OpeningHoursParser.parse(restaurant.getOpeningHours())
            .map(h -> ChatFormatter.time(h.open()) + " - " + ChatFormatter.time(h.close()))
            .orElse(restaurant.getOpeningHours() != null ? restaurant.getOpeningHours() : "non renseignés");
    }

    // ---------------------------------------------------------------- events

    private ChatResponse handleBookOrCheckEvent(ValidatedRequest r) {
        ChatIntent intent = r.intent();
        if (r.hasProblems()) {
            return ChatResponse.of(r.problems().get(0), intent, Type.QUESTION);
        }
        Event event = r.event();
        if (event == null) {
            List<Event> upcoming = upcomingEvents();
            if (upcoming.isEmpty()) {
                return ChatResponse.of("Aucun événement n'est prévu pour le moment.", intent, Type.ANSWER);
            }
            return ChatResponse.of("Voici les événements à venir : " + eventList(upcoming)
                + ". Lequel vous intéresse ?", intent, Type.QUESTION);
        }
        int remaining = eventBookingService.remainingSpots(event);
        if (intent == ChatIntent.CHECK_EVENTS) {
            String reply = remaining > 0
                ? "Il reste " + remaining + " place" + (remaining > 1 ? "s" : "") + " pour « " + event.getTitle()
                    + "» le " + dateTime(event.getEventDate()) + "."
                : "Il ne reste plus de place pour « " + event.getTitle() + " ».";
            return ChatResponse.of(reply, intent, Type.ANSWER);
        }
        if (r.partySize() == null) {
            return ChatResponse.of("Pour combien de personnes souhaitez-vous réserver « " + event.getTitle() + " » ?",
                intent, Type.QUESTION);
        }
        if (event.getEventDate() != null && !event.getEventDate().isAfter(LocalDateTime.now(clock))) {
            return ChatResponse.of("« " + event.getTitle() + " » est déjà passé.", intent, Type.ANSWER);
        }
        if (remaining < r.partySize()) {
            return ChatResponse.of("Désolé, il ne reste que " + remaining + " place" + (remaining != 1 ? "s" : "")
                + " pour « " + event.getTitle() + " », pas assez pour " + persons(r.partySize()) + ".", intent, Type.ANSWER);
        }
        String reply = "« " + event.getTitle() + " » le " + dateTime(event.getEventDate()) + ", "
            + persons(r.partySize()) + ". Confirmez-vous la réservation ?";
        return ChatResponse.of(reply, intent, Type.PROPOSAL)
            .withEventProposal(new EventBookingProposal(EventProposalAction.BOOK, null, event.getId(),
                event.getTitle(), event.getEventDate(), r.partySize()));
    }

    private ChatResponse handleCancelEventBooking(String userId, ValidatedRequest r) {
        List<EventBooking> upcoming = upcomingEventBookings(userId);
        if (upcoming.isEmpty()) {
            return ChatResponse.of("Vous n'avez aucune réservation d'événement à venir à annuler.",
                ChatIntent.CANCEL_EVENT_BOOKING, Type.ANSWER);
        }
        Map<String, Event> events = eventsById();
        EventBooking target = resolveEventTarget(upcoming, r, events);
        if (target == null) {
            return ChatResponse.of("Vous avez plusieurs réservations d'événements à venir. Laquelle souhaitez-vous "
                    + "annuler ? Indiquez son numéro.", ChatIntent.CANCEL_EVENT_BOOKING, Type.QUESTION)
                .withEventBookings(eventSummaries(upcoming, events));
        }
        Event event = events.get(target.getEventId());
        String title = event != null ? event.getTitle() : "cet événement";
        return ChatResponse.of("Voulez-vous vraiment annuler votre réservation pour « " + title + "» ("
                + persons(target.getPartySize()) + ") ?", ChatIntent.CANCEL_EVENT_BOOKING, Type.PROPOSAL)
            .withEventProposal(new EventBookingProposal(EventProposalAction.CANCEL, target.getId(), target.getEventId(),
                title, event != null ? event.getEventDate() : null, target.getPartySize()));
    }

    private ChatResponse handleEventStatus(String userId, ValidatedRequest r) {
        List<EventBooking> upcoming = upcomingEventBookings(userId);
        Map<String, Event> events = eventsById();
        if (upcoming.isEmpty()) {
            return ChatResponse.of("Vous n'avez aucune réservation d'événement à venir. Souhaitez-vous réserver un "
                + "événement ?", ChatIntent.EVENT_STATUS, Type.ANSWER);
        }
        String reply = upcoming.size() == 1
            ? "Vous avez 1 réservation d'événement à venir :"
            : "Vous avez " + upcoming.size() + " réservations d'événements à venir :";
        return ChatResponse.of(reply, ChatIntent.EVENT_STATUS, Type.ANSWER).withEventBookings(eventSummaries(upcoming, events));
    }

    private EventBooking resolveEventTarget(List<EventBooking> upcoming, ValidatedRequest r, Map<String, Event> events) {
        if (r.reservationNumber() != null && r.reservationNumber() <= upcoming.size()) {
            return upcoming.get(r.reservationNumber() - 1);
        }
        if (r.eventNamed() && r.event() != null) {
            List<EventBooking> matching = upcoming.stream()
                .filter(b -> b.getEventId().equals(r.event().getId()))
                .toList();
            if (matching.size() == 1) {
                return matching.get(0);
            }
        }
        return upcoming.size() == 1 ? upcoming.get(0) : null;
    }

    private List<Event> upcomingEvents() {
        LocalDateTime now = LocalDateTime.now(clock);
        return eventRepository.findAll().stream()
            .filter(e -> e.getEventDate() != null && e.getEventDate().isAfter(now))
            .sorted(Comparator.comparing(Event::getEventDate))
            .toList();
    }

    private List<EventBooking> upcomingEventBookings(String userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<String, Event> events = eventsById();
        return eventBookingService.getUserBookings(userId).stream()
            .filter(b -> EventBookingService.ACTIVE_BOOKING_STATUSES.contains(b.getStatus()))
            .filter(b -> {
                Event event = events.get(b.getEventId());
                return event != null && event.getEventDate() != null && event.getEventDate().isAfter(now);
            })
            .sorted(Comparator.comparing(b -> events.get(b.getEventId()).getEventDate()))
            .toList();
    }

    private Map<String, Event> eventsById() {
        return eventRepository.findAll().stream()
            .collect(Collectors.toMap(Event::getId, Function.identity(), (a, b) -> a));
    }

    private String eventList(List<Event> events) {
        return events.stream()
            .map(e -> e.getTitle() + " (" + dateTime(e.getEventDate()) + ")")
            .collect(Collectors.joining(", "));
    }

    private static List<EventBookingSummary> eventSummaries(List<EventBooking> shown, Map<String, Event> events) {
        return shown.stream()
            .map(b -> {
                Event event = events.get(b.getEventId());
                return new EventBookingSummary(shown.indexOf(b) + 1, b.getId(), b.getEventId(),
                    event != null ? event.getTitle() : null, event != null ? event.getEventDate() : null,
                    b.getPartySize(), b.getStatus());
            })
            .toList();
    }
    
    private ChatResponse outOfScope() {
        return ChatResponse.of(OUT_OF_SCOPE_REPLY, ChatIntent.OUT_OF_SCOPE, Type.OUT_OF_SCOPE);
    }
    
    // ---------------------------------------------------------------- confirmation (the only write path)
    
    public ChatResponse confirm(String userId, ChatConfirmRequest request) {
        Map<String, Restaurant> restaurants = restaurantsById();
        try {
            return switch (request.action()) {
                case CREATE -> {
                    requireSlot(request);
                    Reservation reservation = new Reservation();
                    reservation.setRestaurantId(request.restaurantId());
                    reservation.setReservationDateTime(request.reservationDateTime());
                    reservation.setPartySize(request.partySize());
                    reservation.setSpecialRequests(request.specialRequests());
                    Reservation saved = reservationService.createReservation(userId, reservation);
                    yield confirmed("Votre réservation est enregistrée : " + describe(saved, restaurants)
                        + ". Statut : " + ChatFormatter.status(saved.getStatus()) + ".", ChatIntent.CREATE, saved, restaurants);
                }
                case MODIFY -> {
                    requireReservationId(request);
                    requireSlot(request);
                    Reservation saved = reservationService.updateReservation(request.reservationId(), userId,
                        request.restaurantId(), request.reservationDateTime(), request.partySize(), request.specialRequests());
                    yield confirmed("Votre réservation est modifiée : " + describe(saved, restaurants) + ".",
                        ChatIntent.MODIFY, saved, restaurants);
                }
                case CANCEL -> {
                    requireReservationId(request);
                    Reservation saved = reservationService.cancelReservation(request.reservationId(), userId);
                    yield confirmed("Votre réservation " + describe(saved, restaurants) + " est annulée.",
                        ChatIntent.CANCEL, saved, restaurants);
                }
            };
        } catch (ConflictException | BadRequestException e) {
            // e.g. the slot was taken between the proposal and the click: report the real refusal
            return ChatResponse.of("La demande n'a pas pu être enregistrée : " + e.getMessage() + ".",
                intentOf(request.action()), Type.ANSWER);
        }
    }
    
    public ChatResponse confirmEvent(String userId, EventChatConfirmRequest request) {
        try {
            return switch (request.action()) {
                case BOOK -> {
                    if (request.eventId() == null || request.partySize() == null) {
                        throw new BadRequestException("informations de réservation incomplètes");
                    }
                    EventBooking saved = eventBookingService.bookEvent(userId, request.eventId(), request.partySize());
                    Event event = eventRepository.findById(saved.getEventId()).orElse(null);
                    String title = event != null ? event.getTitle() : "cet événement";
                    yield confirmedEvent("Votre réservation pour « " + title + " » est enregistrée ("
                        + persons(saved.getPartySize()) + "). Statut : "
                        + ChatFormatter.eventBookingStatus(saved.getStatus()) + ".", ChatIntent.BOOK_EVENT, saved, event);
                }
                case CANCEL -> {
                    if (request.bookingId() == null || request.bookingId().isBlank()) {
                        throw new BadRequestException("réservation non précisée");
                    }
                    EventBooking saved = eventBookingService.cancelBooking(request.bookingId(), userId);
                    Event event = eventRepository.findById(saved.getEventId()).orElse(null);
                    String title = event != null ? event.getTitle() : "cet événement";
                    yield confirmedEvent("Votre réservation pour « " + title + " » est annulée.",
                        ChatIntent.CANCEL_EVENT_BOOKING, saved, event);
                }
            };
        } catch (ConflictException | BadRequestException e) {
            ChatIntent intent = request.action() == EventProposalAction.BOOK
                ? ChatIntent.BOOK_EVENT : ChatIntent.CANCEL_EVENT_BOOKING;
            return ChatResponse.of("La demande n'a pas pu être enregistrée : " + e.getMessage() + ".", intent, Type.ANSWER);
        }
    }

    private ChatResponse confirmedEvent(String reply, ChatIntent intent, EventBooking saved, Event event) {
        return ChatResponse.of(reply, intent, Type.CONFIRMED)
            .withEventBookings(List.of(new EventBookingSummary(1, saved.getId(), saved.getEventId(),
                event != null ? event.getTitle() : null, event != null ? event.getEventDate() : null,
                saved.getPartySize(), saved.getStatus())));
    }

    private ChatResponse confirmed(String reply, ChatIntent intent, Reservation saved, Map<String, Restaurant> restaurants) {
        Restaurant restaurant = restaurants.get(saved.getRestaurantId());
        return ChatResponse.of(reply, intent, Type.CONFIRMED)
            .withReservations(List.of(new ReservationSummary(1, saved.getId(), saved.getRestaurantId(),
                restaurant != null ? restaurant.getName() : null, saved.getReservationDateTime(),
                saved.getPartySize(), saved.getStatus())));
    }
    
    private static void requireSlot(ChatConfirmRequest request) {
        if (request.restaurantId() == null || request.reservationDateTime() == null || request.partySize() == null) {
            throw new BadRequestException("informations de réservation incomplètes");
        }
    }
    
    private static void requireReservationId(ChatConfirmRequest request) {
        if (request.reservationId() == null || request.reservationId().isBlank()) {
            throw new BadRequestException("réservation non précisée");
        }
    }
    
    private static ChatIntent intentOf(ProposalAction action) {
        return switch (action) {
            case CREATE -> ChatIntent.CREATE;
            case MODIFY -> ChatIntent.MODIFY;
            case CANCEL -> ChatIntent.CANCEL;
        };
    }
    
    private static String describe(Reservation reservation, Map<String, Restaurant> restaurants) {
        Restaurant restaurant = restaurants.get(reservation.getRestaurantId());
        return "chez " + (restaurant != null ? restaurant.getName() : "le restaurant") + " le "
            + dateTime(reservation.getReservationDateTime()) + " pour " + persons(reservation.getPartySize());
    }
    
    // ---------------------------------------------------------------- data helpers
    
    /** Active reservations in the future, sorted by date: their position is the number shown to the user. */
    private List<Reservation> upcomingReservations(String userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return reservationService.getUserReservations(userId).stream()
            .filter(r -> AvailabilityService.ACTIVE_STATUSES.contains(r.getStatus()))
            .filter(r -> r.getReservationDateTime() != null && r.getReservationDateTime().isAfter(now))
            .sorted(Comparator.comparing(Reservation::getReservationDateTime))
            .toList();
    }
    
    private Map<String, Restaurant> restaurantsById() {
        return restaurantRepository.findAll().stream()
            .collect(Collectors.toMap(Restaurant::getId, Function.identity(), (a, b) -> a));
    }
    
    private String restaurantNames() {
        return restaurantRepository.findAll().stream().map(Restaurant::getName).collect(Collectors.joining(", "));
    }
    
    private static List<ReservationSummary> summaries(List<Reservation> reservations, Map<String, Restaurant> restaurants) {
        return summaries(reservations, restaurants, reservations);
    }
    
    // Numbers always refer to the position in the full upcoming list, so « la 2 » stays consistent
    private static List<ReservationSummary> summaries(List<Reservation> shown, Map<String, Restaurant> restaurants,
                                                      List<Reservation> numbering) {
        return shown.stream()
            .map(r -> {
                Restaurant restaurant = restaurants.get(r.getRestaurantId());
                return new ReservationSummary(numbering.indexOf(r) + 1, r.getId(), r.getRestaurantId(),
                    restaurant != null ? restaurant.getName() : null, r.getReservationDateTime(), r.getPartySize(),
                    r.getStatus());
            })
            .toList();
    }
}
