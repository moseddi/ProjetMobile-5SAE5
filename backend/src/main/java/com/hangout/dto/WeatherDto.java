package com.hangout.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherDto {
    private boolean available;
    private String message;
    private String date;
    private Double temperature;
    private Double tempMin;
    private Double tempMax;
    private Integer precipitationProbability;
    private Double windSpeed;
    private Integer weatherCode;
    private String condition;
    private String icon;
}