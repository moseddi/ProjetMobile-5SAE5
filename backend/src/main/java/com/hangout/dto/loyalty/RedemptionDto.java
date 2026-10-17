package com.hangout.dto.loyalty;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedemptionDto {
    private String id;
    private String rewardId;
    private String rewardName;
    private Integer pointsCost;
    private String status;
    private LocalDateTime createdAt;
}
