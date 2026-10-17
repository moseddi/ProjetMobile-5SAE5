package com.hangout.dto.loyalty;

import lombok.Data;

import java.util.List;

@Data
public class MemoryStartResponse {
    private String sessionId;
    private Integer pairCount;
    private List<String> symbols; // shuffled, duplicated pairs — client renders the board
}
