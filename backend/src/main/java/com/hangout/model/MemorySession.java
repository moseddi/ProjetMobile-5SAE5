package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Server-issued session for the Memory Match game. The card layout (pair count)
 * is recorded so the completion claim (moves, duration) can be sanity-checked
 * against a real session instead of trusting the client blindly.
 */
@Data
@Document(collection = "memory_sessions")
public class MemorySession {
    @Id
    private String id;

    private String userId;
    private Integer pairCount;
    private List<String> symbols;
    private Boolean completed = false;

    @CreatedDate
    private LocalDateTime startedAt;
}
