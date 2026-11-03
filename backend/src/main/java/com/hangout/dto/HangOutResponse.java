package com.hangout.dto;

import com.hangout.model.HangOut;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HangOutResponse {
    private String id;
    private String creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String title;
    private String description;
    private LocalDateTime plannedDateTime;
    private Integer maxParticipants;
    private String status;

    /** The viewer's own request on this hangout: PENDING, ACCEPTED, DECLINED, or null if they never asked. */
    private String myRequestStatus;
    private List<ParticipantBrief> participants; // accepted members
    private List<ParticipantBrief> pendingRequests; // join requests awaiting a decision
    private LocalDateTime createdAt;

    @Data
    public static class ParticipantBrief {
        private String id;
        private String userId;
        private String userName;
        private String userAvatar;
        private String status;
        private LocalDateTime requestedAt;
    }
}
