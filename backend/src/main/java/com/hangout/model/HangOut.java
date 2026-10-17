package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "hangouts")
public class HangOut {
    @Id
    private String id;
    
    private String creatorId;
    private String restaurantId;
    private String title;
    private String description;
    private LocalDateTime plannedDateTime;
    private Integer maxParticipants;
    private List<Participant> participants;
    private HangOutStatus status = HangOutStatus.OPEN;
    private Boolean aiGenerated = false;

    @CreatedDate
    private LocalDateTime createdAt;
    
    @Data
    public static class Participant {
        private String userId;
        private ParticipantStatus status;
    }
    
    public enum ParticipantStatus {
        PENDING, ACCEPTED, DECLINED
    }
    
    public enum HangOutStatus {
        OPEN, FULL, CANCELLED, COMPLETED
    }
}
