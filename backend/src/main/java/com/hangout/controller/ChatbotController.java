package com.hangout.controller;

import com.hangout.chatbot.ChatbotOrchestrator;
import com.hangout.dto.chat.ChatConfirmRequest;
import com.hangout.dto.chat.ChatMessageRequest;
import com.hangout.dto.chat.ChatResponse;
import com.hangout.dto.chat.EventChatConfirmRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reservation assistant. Requires a valid JWT (see SecurityConfig); the user id comes from the token.
 */
@RestController
@RequestMapping("/api/chatbot/reservation")
@RequiredArgsConstructor
public class ChatbotController {
    
    private final ChatbotOrchestrator orchestrator;
    
    /** Analyses a message. Never writes to the database. */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> message(@AuthenticationPrincipal String userId,
                                                @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(orchestrator.handleMessage(userId, request));
    }
    
    /** Executes a proposal the user explicitly confirmed, through ReservationService (re-validated). */
    @PostMapping("/confirm")
    public ResponseEntity<ChatResponse> confirm(@AuthenticationPrincipal String userId,
                                                @Valid @RequestBody ChatConfirmRequest request) {
        return ResponseEntity.ok(orchestrator.confirm(userId, request));
    }

    /** Executes an event-booking proposal the user explicitly confirmed, through EventBookingService (re-validated). */
    @PostMapping("/confirm-event")
    public ResponseEntity<ChatResponse> confirmEvent(@AuthenticationPrincipal String userId,
                                                      @Valid @RequestBody EventChatConfirmRequest request) {
        return ResponseEntity.ok(orchestrator.confirmEvent(userId, request));
    }
}
