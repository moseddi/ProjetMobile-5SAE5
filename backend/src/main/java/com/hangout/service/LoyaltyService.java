package com.hangout.service;

import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Notification;
import com.hangout.model.PointTransaction;
import com.hangout.model.User;
import com.hangout.repository.PointTransactionRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void awardPoints(String userId, int points, PointTransaction.TransactionType type,
                             String description, String relatedId) {
        if (points <= 0) return;

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setLoyaltyPoints(user.getLoyaltyPoints() + points);
        userRepository.save(user);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setPoints(points);
        tx.setType(type);
        tx.setDescription(description);
        tx.setRelatedId(relatedId);
        pointTransactionRepository.save(tx);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("You earned " + points + " points!");
        notification.setMessage(description);
        notification.setType(Notification.NotificationType.LOYALTY);
        notification.setRelatedId(relatedId);
        notificationService.createNotification(notification);
    }

    @Transactional
    public void redeemPoints(String userId, int points, String description) {
        if (points <= 0) {
            throw new BadRequestException("Points to redeem must be greater than zero");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getLoyaltyPoints() < points) {
            throw new BadRequestException("Not enough loyalty points");
        }

        user.setLoyaltyPoints(user.getLoyaltyPoints() - points);
        userRepository.save(user);

        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setPoints(-points);
        tx.setType(PointTransaction.TransactionType.REDEEM);
        tx.setDescription(description);
        pointTransactionRepository.save(tx);
    }

    public List<PointTransaction> getHistory(String userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public int getBalance(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getLoyaltyPoints();
    }
}
