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
import java.util.Map;

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

    // ===== NEW ENDPOINTS FOR MAP & MEETING POINT =====

    /**
     * Update meeting point details for a hangout.
     * Only the creator can update this.
     * @param id Hangout ID
     * @param meetingPointData Map with: meetingPointName, meetingPointLatitude, meetingPointLongitude
     * @return Updated HangOutResponse
     */
    @PutMapping("/{id}/meeting-point")
    public ResponseEntity<HangOutResponse> updateMeetingPoint(
            @PathVariable String id,
            @RequestBody Map<String, Object> meetingPointData) {
        return ResponseEntity.ok(hangOutService.updateMeetingPoint(id, SecurityUtils.getCurrentUserId(), meetingPointData));
    }

    /**
     * Update restaurant GPS coordinates for a hangout.
     * Only the creator can update this.
     * @param id Hangout ID
     * @param coordinatesData Map with: restaurantLatitude, restaurantLongitude
     * @return Updated HangOutResponse
     */
    @PutMapping("/{id}/restaurant-coordinates")
    public ResponseEntity<HangOutResponse> updateRestaurantCoordinates(
            @PathVariable String id,
            @RequestBody Map<String, Object> coordinatesData) {
        return ResponseEntity.ok(hangOutService.updateRestaurantCoordinates(id, SecurityUtils.getCurrentUserId(), coordinatesData));
    }
}
