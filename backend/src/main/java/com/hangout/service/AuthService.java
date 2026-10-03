package com.hangout.service;

import com.hangout.dto.AuthRequest;
import com.hangout.dto.AuthResponse;
import com.hangout.dto.RegisterRequest;
import com.hangout.dto.UserDto;
import com.hangout.exception.BadRequestException;
import com.hangout.model.User;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setLoyaltyPoints(0);
        user.setTotalReservations(0);
        user.setTotalOrders(0);
        user.setTotalReviews(0);
        
        user = userRepository.save(user);
        
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toDto(user));
    }
    
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toDto(user));
    }
    
    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setTotalReservations(user.getTotalReservations());
        dto.setTotalOrders(user.getTotalOrders());
        dto.setTotalReviews(user.getTotalReviews());
        return dto;
    }
}
