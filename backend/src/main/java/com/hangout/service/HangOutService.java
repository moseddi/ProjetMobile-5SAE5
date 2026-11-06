package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.config.SecurityUtils;
import com.hangout.dto.HangOutRequest;
import com.hangout.dto.HangOutResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.HangOut;
import com.hangout.model.Notification;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.HangOutRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HangOutService {

    private final HangOutRepository hangOutRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final NotificationService notificationService;

    public List<HangOutResponse> getAll() {
        return hangOutRepository.findAll().stream().map(this::normalize)
            .sorted(Newest.first(HangOut::getId)).map(this::toResponse).collect(Collectors.toList());
    }

    public HangOutResponse getResponseById(String id) {
        return toResponse(getOrThrow(id));
    }

    public List<HangOutResponse> getUserHangOuts(String userId) {
        return hangOutRepository.findByCreatorId(userId).stream().map(this::normalize)
            .sorted(Newest.first(HangOut::getId))
            .map(this::toResponse).collect(Collectors.toList());
    }

    public List<HangOutResponse> getUserPendingRequests(String userId) {
        return hangOutRepository.findAll().stream().map(this::normalize)
            .filter(h -> h.getParticipants() != null && h.getParticipants().stream()
                .anyMatch(p -> userId.equals(p.getUserId()) && p.getStatus() == HangOut.ParticipantStatus.PENDING))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public HangOutResponse create(String creatorId, HangOutRequest request) {
        restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        HangOut hangOut = new HangOut();
        hangOut.setCreatorId(creatorId);
        hangOut.setRestaurantId(request.getRestaurantId());
        hangOut.setTitle(request.getTitle());
        hangOut.setDescription(request.getDescription());
        hangOut.setPlannedDateTime(request.getPlannedDateTime());
        hangOut.setMaxParticipants(request.getMaxParticipants());
        hangOut.setParticipants(new ArrayList<>());
        hangOut.setStatus(HangOut.HangOutStatus.OPEN);
        
        // Set GPS coordinates
        hangOut.setRestaurantLatitude(request.getRestaurantLatitude());
        hangOut.setRestaurantLongitude(request.getRestaurantLongitude());
        
        // Set meeting point if provided
        hangOut.setMeetingPointName(request.getMeetingPointName());
        hangOut.setMeetingPointLatitude(request.getMeetingPointLatitude());
        hangOut.setMeetingPointLongitude(request.getMeetingPointLongitude());

        return toResponse(hangOutRepository.save(hangOut));
    }

    @Transactional
    public HangOutResponse requestToJoin(String hangOutId, String userId) {
        HangOut hangOut = getOrThrow(hangOutId);

        if (hangOut.getCreatorId().equals(userId)) {
            throw new BadRequestException("You cannot join your own hangout");
        }
        if (hangOut.getPlannedDateTime() != null && hangOut.getPlannedDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This hangout has already taken place");
        }
        if (hangOut.getStatus() != HangOut.HangOutStatus.OPEN) {
            throw new BadRequestException("This hangout is not open for new requests");
        }

        if (hangOut.getParticipants() == null) {
            hangOut.setParticipants(new ArrayList<>());
        }
        List<HangOut.Participant> participants = hangOut.getParticipants();

        HangOut.Participant previous = participants.stream()
            .filter(p -> userId.equals(p.getUserId()))
            .findFirst()
            .orElse(null);
        if (previous != null) {
            switch (previous.getStatus()) {
                case ACCEPTED -> throw new BadRequestException("You already joined this hangout");
                case DECLINED -> throw new BadRequestException("The organizer declined your request for this hangout");
                default -> throw new BadRequestException("You already asked to join - wait for the organizer to answer");
            }
        }

        long acceptedCount = participants.stream()
            .filter(p -> p.getStatus() == HangOut.ParticipantStatus.ACCEPTED).count();
        if (acceptedCount >= hangOut.getMaxParticipants()) {
            throw new BadRequestException("This hangout is full");
        }

        HangOut.Participant participant = new HangOut.Participant();
        participant.setId(UUID.randomUUID().toString());
        participant.setUserId(userId);
        participant.setStatus(HangOut.ParticipantStatus.PENDING);
        participant.setRequestedAt(LocalDateTime.now());
        participants.add(participant);

        HangOut saved = hangOutRepository.save(hangOut);

        User requester = userRepository.findById(userId).orElse(null);
        User organizer = userRepository.findById(hangOut.getCreatorId()).orElse(null);
        String organizerName = organizer != null ? organizer.getName() : "the organizer";

        // Tell the organizer someone wants in...
        notify(hangOut.getCreatorId(), hangOut.getId(), "New hangout request",
            (requester != null ? requester.getName() : "Someone") + " wants to join \"" + hangOut.getTitle()
                + "\". Open My Hang Outs to accept or delete the request.");

        // ...and tell the person who asked to wait for that answer.
        notify(userId, hangOut.getId(), "Request sent - waiting for " + organizerName,
            "You asked to join \"" + hangOut.getTitle() + "\". Please wait for " + organizerName
                + " to accept your request.");

        return toResponse(saved);
    }

    @Transactional
    public HangOutResponse decideRequest(String hangOutId, String participantId, String creatorId, boolean accept) {
        HangOut hangOut = getOrThrow(hangOutId);
        if (!hangOut.getCreatorId().equals(creatorId)) {
            throw new AccessDeniedException("Only the creator can manage join requests");
        }

        HangOut.Participant participant = hangOut.getParticipants().stream()
            .filter(p -> participantId.equals(p.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (participant.getStatus() != HangOut.ParticipantStatus.PENDING) {
            throw new BadRequestException("This request was already handled");
        }

        if (accept) {
            long acceptedCount = hangOut.getParticipants().stream()
                .filter(p -> p.getStatus() == HangOut.ParticipantStatus.ACCEPTED).count();
            if (acceptedCount >= hangOut.getMaxParticipants()) {
                throw new BadRequestException("This hangout is already full");
            }
            participant.setStatus(HangOut.ParticipantStatus.ACCEPTED);

            if (acceptedCount + 1 >= hangOut.getMaxParticipants()) {
                hangOut.setStatus(HangOut.HangOutStatus.FULL);
            }
        } else {
            participant.setStatus(HangOut.ParticipantStatus.DECLINED);
        }

        HangOut saved = hangOutRepository.save(hangOut);

        User organizer = userRepository.findById(hangOut.getCreatorId()).orElse(null);
        String organizerName = organizer != null ? organizer.getName() : "The organizer";

        // The person who asked always hears the decision, whichever way it went.
        notify(participant.getUserId(), hangOut.getId(),
            accept ? "Request accepted!" : "Request declined",
            accept
                ? organizerName + " accepted you for \"" + hangOut.getTitle() + "\" - see you there!"
                : organizerName + " declined your request to join \"" + hangOut.getTitle() + "\".");

        // Everyone already in the group learns about the newcomer.
        if (accept) {
            User newcomer = userRepository.findById(participant.getUserId()).orElse(null);
            String newcomerName = newcomer != null ? newcomer.getName() : "Someone";
            hangOut.getParticipants().stream()
                .filter(p -> p.getStatus() == HangOut.ParticipantStatus.ACCEPTED)
                .filter(p -> !p.getUserId().equals(participant.getUserId()))
                .forEach(member -> notify(member.getUserId(), hangOut.getId(), "New member",
                    newcomerName + " joined \"" + hangOut.getTitle() + "\"."));
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(String hangOutId, String userId) {
        HangOut hangOut = getOrThrow(hangOutId);
        if (!hangOut.getCreatorId().equals(userId)) {
            throw new AccessDeniedException("Only the creator can delete this hangout");
        }
        if (hangOut.getParticipants() != null) {
            hangOut.getParticipants().stream()
                .filter(p -> p.getStatus() != HangOut.ParticipantStatus.DECLINED)
                .forEach(p -> notify(p.getUserId(), null, "Hangout cancelled",
                    "\"" + hangOut.getTitle() + "\" was cancelled by the organizer."));
        }
        hangOutRepository.delete(hangOut);
    }

    @Transactional
    public HangOutResponse updateMeetingPoint(String hangOutId, String userId, java.util.Map<String, Object> meetingPointData) {
        HangOut hangOut = getOrThrow(hangOutId);
        if (!hangOut.getCreatorId().equals(userId)) {
            throw new AccessDeniedException("Only the creator can update the meeting point");
        }

        if (meetingPointData.containsKey("meetingPointName")) {
            hangOut.setMeetingPointName((String) meetingPointData.get("meetingPointName"));
        }
        if (meetingPointData.containsKey("meetingPointLatitude")) {
            Object lat = meetingPointData.get("meetingPointLatitude");
            hangOut.setMeetingPointLatitude(lat instanceof Number ? ((Number) lat).doubleValue() : null);
        }
        if (meetingPointData.containsKey("meetingPointLongitude")) {
            Object lon = meetingPointData.get("meetingPointLongitude");
            hangOut.setMeetingPointLongitude(lon instanceof Number ? ((Number) lon).doubleValue() : null);
        }

        return toResponse(hangOutRepository.save(hangOut));
    }

    @Transactional
    public HangOutResponse updateRestaurantCoordinates(String hangOutId, String userId, java.util.Map<String, Object> coordinatesData) {
        HangOut hangOut = getOrThrow(hangOutId);
        if (!hangOut.getCreatorId().equals(userId)) {
            throw new AccessDeniedException("Only the creator can update restaurant coordinates");
        }

        if (coordinatesData.containsKey("restaurantLatitude")) {
            Object lat = coordinatesData.get("restaurantLatitude");
            hangOut.setRestaurantLatitude(lat instanceof Number ? ((Number) lat).doubleValue() : null);
        }
        if (coordinatesData.containsKey("restaurantLongitude")) {
            Object lon = coordinatesData.get("restaurantLongitude");
            hangOut.setRestaurantLongitude(lon instanceof Number ? ((Number) lon).doubleValue() : null);
        }

        return toResponse(hangOutRepository.save(hangOut));
    }

    private void notify(String userId, String hangOutId, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(Notification.NotificationType.HANGOUT);
        notification.setRelatedId(hangOutId);
        notificationService.createNotification(notification);
    }

    private String myRequestStatus(HangOut hangOut) {
        String me;
        try {
            me = SecurityUtils.getCurrentUserId();
        } catch (RuntimeException e) {
            return null;
        }
        if (hangOut.getParticipants() == null) return null;
        return hangOut.getParticipants().stream()
            .filter(p -> me.equals(p.getUserId()))
            .map(p -> p.getStatus().name())
            .findFirst()
            .orElse(null);
    }

    private HangOut getOrThrow(String id) {
        return normalize(hangOutRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hangout not found")));
    }

    /** Repairs legacy participants (no id / no status) so requests can be addressed, and saves the fix. */
    private HangOut normalize(HangOut hangOut) {
        if (hangOut.getParticipants() == null) return hangOut;
        boolean changed = false;
        hangOut.getParticipants().removeIf(p -> p.getUserId() == null);
        for (HangOut.Participant p : hangOut.getParticipants()) {
            if (p.getId() == null || p.getId().isBlank()) {
                p.setId(UUID.randomUUID().toString());
                changed = true;
            }
            if (p.getStatus() == null) {
                p.setStatus(HangOut.ParticipantStatus.PENDING);
                changed = true;
            }
        }
        if (hangOut.getStatus() == null) {
            hangOut.setStatus(HangOut.HangOutStatus.OPEN);
            changed = true;
        }
        return changed ? hangOutRepository.save(hangOut) : hangOut;
    }

    private HangOutResponse toResponse(HangOut hangOut) {
        Restaurant restaurant = hangOut.getRestaurantId() != null
            ? restaurantRepository.findById(hangOut.getRestaurantId()).orElse(null)
            : null;
        User creator = hangOut.getCreatorId() != null
            ? userRepository.findById(hangOut.getCreatorId()).orElse(null)
            : null;

        HangOutResponse response = new HangOutResponse();
        response.setId(hangOut.getId());
        response.setCreatorId(hangOut.getCreatorId());
        response.setCreatorName(creator != null ? creator.getName() : "Unknown");
        response.setCreatorAvatar(creator != null ? creator.getAvatar() : "");
        response.setRestaurantId(hangOut.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setTitle(hangOut.getTitle());
        response.setDescription(hangOut.getDescription());
        response.setPlannedDateTime(hangOut.getPlannedDateTime());
        response.setMaxParticipants(hangOut.getMaxParticipants());
        response.setStatus(hangOut.getStatus().name());
        response.setMyRequestStatus(myRequestStatus(hangOut));
        response.setCreatedAt(hangOut.getCreatedAt());
        
        // Map GPS coordinates
        response.setRestaurantLatitude(hangOut.getRestaurantLatitude());
        response.setRestaurantLongitude(hangOut.getRestaurantLongitude());
        
        // Map meeting point
        response.setMeetingPointName(hangOut.getMeetingPointName());
        response.setMeetingPointLatitude(hangOut.getMeetingPointLatitude());
        response.setMeetingPointLongitude(hangOut.getMeetingPointLongitude());

        List<HangOut.Participant> participants = hangOut.getParticipants() != null ? hangOut.getParticipants() : List.of();

        response.setParticipants(participants.stream()
            .filter(p -> p.getStatus() == HangOut.ParticipantStatus.ACCEPTED)
            .map(this::toBrief)
            .collect(Collectors.toList()));

        response.setPendingRequests(participants.stream()
            .filter(p -> p.getStatus() == HangOut.ParticipantStatus.PENDING)
            .sorted(Comparator.comparing(HangOut.Participant::getRequestedAt,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toBrief)
            .collect(Collectors.toList()));

        return response;
    }

    private HangOutResponse.ParticipantBrief toBrief(HangOut.Participant participant) {
        User user = participant.getUserId() != null
            ? userRepository.findById(participant.getUserId()).orElse(null)
            : null;
        HangOutResponse.ParticipantBrief brief = new HangOutResponse.ParticipantBrief();
        brief.setId(participant.getId());
        brief.setUserId(participant.getUserId());
        brief.setUserName(user != null ? user.getName() : "Unknown");
        brief.setUserAvatar(user != null ? user.getAvatar() : "");
        brief.setStatus(participant.getStatus().name());
        brief.setRequestedAt(participant.getRequestedAt());
        return brief;
    }
}
