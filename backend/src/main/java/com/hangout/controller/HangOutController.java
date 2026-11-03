package com.hangout.controller;

import com.hangout.config.SecurityUtils;
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
    public ResponseEntity<List<HangOutResponse>> getAll() {
        return ResponseEntity.ok(hangOutService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HangOutResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(hangOutService.getResponseById(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<HangOutResponse>> getMine() {
        return ResponseEntity.ok(hangOutService.getUserHangOuts(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<List<HangOutResponse>> getMyPendingRequests() {
        return ResponseEntity.ok(hangOutService.getUserPendingRequests(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<HangOutResponse> create(@Valid @RequestBody HangOutRequest request) {
        return ResponseEntity.ok(hangOutService.create(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<HangOutResponse> requestToJoin(@PathVariable String id) {
        return ResponseEntity.ok(hangOutService.requestToJoin(id, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/requests/{participantId}/accept")
    public ResponseEntity<HangOutResponse> accept(@PathVariable String id, @PathVariable String participantId) {
        return ResponseEntity.ok(hangOutService.decideRequest(id, participantId, SecurityUtils.getCurrentUserId(), true));
    }

    @PutMapping("/{id}/requests/{participantId}/decline")
    public ResponseEntity<HangOutResponse> decline(@PathVariable String id, @PathVariable String participantId) {
        return ResponseEntity.ok(hangOutService.decideRequest(id, participantId, SecurityUtils.getCurrentUserId(), false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        hangOutService.delete(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
