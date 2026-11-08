package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** A reward a client bought with restaurant points; shown to the restaurant to be honoured. */
@Data
@Document(collection = "vouchers")
public class Voucher {
    @Id
    private String id;

    private String code;
    private String userId;
    private String restaurantId;
    private RewardType rewardType;
    private Integer pointsSpent;
    private Status status = Status.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    public enum Status {
        ACTIVE, USED
    }
}
