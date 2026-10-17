package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "redemptions")
public class Redemption {
    @Id
    private String id;

    private String userId;
    private String rewardId;
    private String rewardName;
    private Integer pointsCost;
    private RedemptionStatus status = RedemptionStatus.COMPLETED;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum RedemptionStatus {
        COMPLETED, CANCELLED
    }
}
