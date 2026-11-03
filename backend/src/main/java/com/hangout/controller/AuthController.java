package com.hangout.controller;

import com.hangout.config.SecurityUtils;
import com.hangout.dto.AuthRequest;
import com.hangout.dto.AuthResponse;
import com.hangout.dto.OwnerApplicationRequest;
import com.hangout.dto.OwnerApplicationResponse;
import com.hangout.dto.RegisterRequest;
import com.hangout.dto.UserDto;
import com.hangout.dto.UserMapper;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.repository.UserRepository;
import com.hangout.service.AuthService;
import com.hangout.service.OwnerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final OwnerApplicationService ownerApplicationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Public: a restaurant owner asks to join the platform; an admin must accept it. */
    @PostMapping("/owner-application")
    public ResponseEntity<OwnerApplicationResponse> applyAsOwner(@Valid @RequestBody OwnerApplicationRequest request) {
        return ResponseEntity.ok(ownerApplicationService.submit(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        String userId = SecurityUtils.getCurrentUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserMapper.toDto(user));
    }
}
