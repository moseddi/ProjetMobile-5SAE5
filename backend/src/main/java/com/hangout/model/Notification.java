package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean read = false;
    private String relatedId; // ID of related entity (reservation, order, etc.)
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum NotificationType {
        RESERVATION, ORDER, LOYALTY, EVENT, HANGOUT, REVIEW, SYSTEM
    }
}
