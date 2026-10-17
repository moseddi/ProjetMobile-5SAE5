package com.hangout.dto;

import lombok.Data;

@Data
public class ParticipantDto {
    private String userId;
    private String name;
    private String avatar;
    private String status;
}
