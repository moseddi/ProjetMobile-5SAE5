package com.hangout.controller;

import com.hangout.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record ChatRequest(
            @NotBlank(message = "A question is required")
            @Size(max = 500, message = "A question must be at most 500 characters")
            String message) {
    }

    @PostMapping
    public ResponseEntity<ChatService.ChatReply> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.ask(request.message().trim()));
    }
}