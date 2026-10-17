package com.hangout.dto.loyalty;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointTransactionDto {
    private String id;
    private Integer points;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
