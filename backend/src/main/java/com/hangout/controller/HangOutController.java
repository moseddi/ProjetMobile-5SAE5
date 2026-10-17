package com.hangout.controller;

import com.hangout.dto.HangOutRequest;
import com.hangout.dto.HangOutResponse;
import com.hangout.service.HangOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hangouts")
@RequiredArgsConstructor
public class HangOutController {

    private final HangOutService hangOutService;

    @GetMapping
    public ResponseEntity<List<HangOutResponse>> getAllHangOuts(
            @RequestParam(defaultValue = "false") boolean upcoming) {
        return ResponseEntity.ok(hangOutService.getAllHangOuts(upcoming));
    }

    @GetMapping("/creator/{userId}")
    public ResponseEntity<List<HangOutResponse>> getHangOutsByCreator(@PathVariable String userId) {
        return ResponseEntity.ok(hangOutService.getHangOutsByCreator(userId));
    }

    @GetMapping("/joined/{userId}")
    public ResponseEntity<List<HangOutResponse>> getHangOutsJoinedByUser(@PathVariable String userId) {
        return ResponseEntity.ok(hangOutService.getHangOutsJoinedByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HangOutResponse> getHangOutById(@PathVariable String id) {
        return ResponseEntity.ok(hangOutService.getHangOutById(id));
    }

    @PostMapping
    public ResponseEntity<HangOutResponse> createHangOut(
            @RequestParam String userId,
            @Valid @RequestBody HangOutRequest request) {
        return ResponseEntity.ok(hangOutService.createHangOut(userId, request));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<HangOutResponse> joinHangOut(
            @PathVariable String id,
            @RequestParam String userId) {
        return ResponseEntity.ok(hangOutService.joinHangOut(id, userId));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<HangOutResponse> leaveHangOut(
            @PathVariable String id,
            @RequestParam String userId) {
        return ResponseEntity.ok(hangOutService.leaveHangOut(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelHangOut(
            @PathVariable String id,
            @RequestParam String userId) {
        hangOutService.cancelHangOut(id, userId);
        return ResponseEntity.ok().build();
    }
}
