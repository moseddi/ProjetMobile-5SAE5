package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "point_transactions")
public class PointTransaction {
    @Id
    private String id;
    
    private String userId;
    private Integer points;
    private TransactionType type;
    private String description;
    private String relatedId;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum TransactionType {
        EARN_RESERVATION, EARN_ORDER, EARN_REVIEW, EARN_GAME, EARN_CHALLENGE, REDEEM, ADJUSTMENT
    }
}
