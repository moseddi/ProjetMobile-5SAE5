package com.hangout.service;

import com.hangout.dto.HangOutRequest;
import com.hangout.dto.HangOutResponse;
import com.hangout.dto.ParticipantDto;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.HangOut;
import com.hangout.model.Restaurant;
import com.hangout.model.User;
import com.hangout.repository.HangOutRepository;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HangOutService {

    private final HangOutRepository hangOutRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public List<HangOutResponse> getAllHangOuts(boolean upcomingOnly) {
        List<HangOut> hangOuts;
        if (upcomingOnly) {
            hangOuts = hangOutRepository.findByStatusInOrderByPlannedDateTimeAsc(
                List.of(HangOut.HangOutStatus.OPEN, HangOut.HangOutStatus.FULL));
        } else {
            hangOuts = hangOutRepository.findAllByOrderByPlannedDateTimeAsc();
        }
        return hangOuts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public HangOutResponse getHangOutById(String id) {
        return toResponse(find(id));
    }

    public List<HangOutResponse> getHangOutsByCreator(String userId) {
        return hangOutRepository.findByCreatorId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<HangOutResponse> getHangOutsJoinedByUser(String userId) {
        return hangOutRepository.findByParticipantsUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public HangOutResponse createHangOut(String userId, HangOutRequest request) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        HangOut hangOut = new HangOut();
        hangOut.setCreatorId(userId);
        hangOut.setRestaurantId(restaurant.getId());
        hangOut.setTitle(request.getTitle());
        hangOut.setDescription(request.getDescription());
        hangOut.setPlannedDateTime(request.getPlannedDateTime());
        hangOut.setMaxParticipants(request.getMaxParticipants());
        hangOut.setAiGenerated(Boolean.TRUE.equals(request.getAiGenerated()));
        hangOut.setStatus(HangOut.HangOutStatus.OPEN);

        List<HangOut.Participant> participants = new ArrayList<>();
        HangOut.Participant creator = new HangOut.Participant();
        creator.setUserId(userId);
        creator.setStatus(HangOut.ParticipantStatus.ACCEPTED);
        participants.add(creator);
        hangOut.setParticipants(participants);

        return toResponse(hangOutRepository.save(hangOut));
    }

    @Transactional
    public HangOutResponse joinHangOut(String hangOutId, String userId) {
        HangOut hangOut = find(hangOutId);
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (hangOut.getStatus() == HangOut.HangOutStatus.CANCELLED) {
            throw new BadRequestException("This hangout has been cancelled");
        }
        if (hangOut.getStatus() == HangOut.HangOutStatus.COMPLETED) {
            throw new BadRequestException("This hangout is already completed");
        }

        List<HangOut.Participant> participants = hangOut.getParticipants() != null
            ? hangOut.getParticipants() : new ArrayList<>();

        boolean alreadyJoined = participants.stream()
            .anyMatch(p -> userId.equals(p.getUserId()));
        if (alreadyJoined) {
            throw new BadRequestException("You have already joined this hangout");
        }

        int max = hangOut.getMaxParticipants() != null ? hangOut.getMaxParticipants() : Integer.MAX_VALUE;
        if (participants.size() >= max) {
            throw new BadRequestException("This hangout is already full");
        }

        HangOut.Participant participant = new HangOut.Participant();
        participant.setUserId(userId);
        participant.setStatus(HangOut.ParticipantStatus.ACCEPTED);
        participants.add(participant);
        hangOut.setParticipants(participants);

        if (participants.size() >= max) {
            hangOut.setStatus(HangOut.HangOutStatus.FULL);
        }
        return toResponse(hangOutRepository.save(hangOut));
    }

    @Transactional
    public HangOutResponse leaveHangOut(String hangOutId, String userId) {
        HangOut hangOut = find(hangOutId);
        if (userId.equals(hangOut.getCreatorId())) {
            throw new BadRequestException("The organizer cannot leave. Cancel the hangout instead.");
        }

        List<HangOut.Participant> participants = hangOut.getParticipants() != null
            ? hangOut.getParticipants() : new ArrayList<>();
        boolean removed = participants.removeIf(p -> userId.equals(p.getUserId()));
        if (!removed) {
            throw new BadRequestException("You are not a participant of this hangout");
        }
        hangOut.setParticipants(participants);

        if (hangOut.getStatus() == HangOut.HangOutStatus.FULL) {
            hangOut.setStatus(HangOut.HangOutStatus.OPEN);
        }
        return toResponse(hangOutRepository.save(hangOut));
    }

    @Transactional
    public void cancelHangOut(String hangOutId, String userId) {
        HangOut hangOut = find(hangOutId);
        if (!userId.equals(hangOut.getCreatorId())) {
            throw new BadRequestException("Only the organizer can cancel this hangout");
        }
        hangOut.setStatus(HangOut.HangOutStatus.CANCELLED);
        hangOutRepository.save(hangOut);
    }

    private HangOut find(String id) {
        return hangOutRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hangout not found"));
    }

    private HangOutResponse toResponse(HangOut hangOut) {
        Restaurant restaurant = restaurantRepository.findById(hangOut.getRestaurantId()).orElse(null);
        User creator = userRepository.findById(hangOut.getCreatorId()).orElse(null);

        HangOutResponse response = new HangOutResponse();
        response.setId(hangOut.getId());
        response.setCreatorId(hangOut.getCreatorId());
        response.setCreatorName(creator != null ? creator.getName() : "Unknown");
        response.setCreatorAvatar(creator != null ? creator.getAvatar() : "");
        response.setRestaurantId(hangOut.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setRestaurantCuisine(restaurant != null ? restaurant.getCuisine() : "");
        response.setRestaurantPriceRange(restaurant != null && restaurant.getPriceRange() != null
            ? restaurant.getPriceRange().name() : "");
        response.setTitle(hangOut.getTitle());
        response.setDescription(hangOut.getDescription());
        response.setPlannedDateTime(hangOut.getPlannedDateTime());
        response.setMaxParticipants(hangOut.getMaxParticipants());
        response.setStatus(hangOut.getStatus() != null ? hangOut.getStatus().name() : "OPEN");
        response.setAiGenerated(Boolean.TRUE.equals(hangOut.getAiGenerated()));
        response.setCreatedAt(hangOut.getCreatedAt());

        List<ParticipantDto> participantDtos = new ArrayList<>();
        if (hangOut.getParticipants() != null) {
            for (HangOut.Participant p : hangOut.getParticipants()) {
                User u = userRepository.findById(p.getUserId()).orElse(null);
                ParticipantDto dto = new ParticipantDto();
                dto.setUserId(p.getUserId());
                dto.setName(u != null ? u.getName() : "Unknown");
                dto.setAvatar(u != null ? u.getAvatar() : "");
                dto.setStatus(p.getStatus() != null ? p.getStatus().name() : "ACCEPTED");
                participantDtos.add(dto);
            }
        }
        response.setParticipants(participantDtos);
        response.setJoinedCount(participantDtos.size());
        return response;
    }
}
