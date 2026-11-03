package com.hangout.service;

import com.hangout.dto.AuthRequest;
import com.hangout.dto.AuthResponse;
import com.hangout.dto.RegisterRequest;
import com.hangout.dto.UserMapper;
import com.hangout.exception.BadRequestException;
import com.hangout.model.User;
import com.hangout.model.OwnerApplication;
import com.hangout.repository.OwnerApplicationRepository;
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
    private final OwnerApplicationRepository ownerApplicationRepository;

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
        // Public self-registration always creates a CLIENT account.
        // RESTAURANT_OWNER and ADMIN accounts are provisioned by an admin.
        user.setRole(User.Role.CLIENT);
        user.setActive(true);
        user.setLoyaltyPoints(0);
        user.setTotalReservations(0);
        user.setTotalOrders(0);
        user.setTotalReviews(0);

        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserMapper.toDto(user));
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail().trim();
        User user = userRepository.findByEmail(email)
            .or(() -> userRepository.findByEmail(email.toLowerCase()))
            .orElse(null);
        if (user == null) {
            explainApplicationState(email);
            throw new BadRequestException("Invalid email or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new BadRequestException("This account has been deactivated. Contact support.");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, UserMapper.toDto(user));
    }

    /** A restaurant owner who has no account yet may simply be waiting for the admin. */
    private void explainApplicationState(String email) {
        ownerApplicationRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(email).ifPresent(a -> {
            if (a.getStatus() == OwnerApplication.Status.PENDING) {
                throw new BadRequestException(
                    "Your restaurant request is still waiting for admin approval. You will be able to log in once it is accepted.");
            }
            if (a.getStatus() == OwnerApplication.Status.REJECTED) {
                throw new BadRequestException("Your restaurant request was declined"
                    + (a.getRejectionReason() != null ? ": " + a.getRejectionReason() : ".")
                    + " Contact the platform team for more information.");
            }
        });
    }
}
