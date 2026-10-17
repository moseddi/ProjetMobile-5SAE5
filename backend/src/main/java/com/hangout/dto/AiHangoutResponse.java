package com.hangout.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiHangoutResponse {
    private String restaurantId;
    private String restaurantName;
    private String title;
    private String description;
    private String suggestedDateTime;
    private List<String> keywords;
    private String reason;
    private Integer suggestedPartySize;
    private boolean aiGenerated;
    private String provider;
    private String notice;
}
