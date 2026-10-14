package com.hangout.service;

import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.User;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==========================================================================
 * ⭐ MODULE : AVIS & INTELLIGENCE RAG (MON TRAVAIL / MY MODULE)
 * Auteur : Responsable Module Avis & RAG
 * Rôle : Gestion des avis, calcul multi-critères, upvotes et statut
 * ==========================================================================
 * Tests Unitaires automatisés (Mockito) pour ReviewService.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private Restaurant mockRestaurant;
    private User mockUser;
    private Review mockReview;

    @BeforeEach
    void setUp() {
        mockRestaurant = new Restaurant();
        mockRestaurant.setId("resto-100");
        mockRestaurant.setName("Beyond Flavours");

        mockUser = new User();
        mockUser.setId("usr-200");
        mockUser.setName("Jane Doe");

        mockReview = new Review();
        mockReview.setId("rev-300");
        mockReview.setRestaurantId("resto-100");
        mockReview.setUserId("usr-200");
        mockReview.setRating(4.5);
        mockReview.setTitle("Excellente expérience");
        mockReview.setComment("La nourriture était savoureuse et le cadre chaleureux.");
        mockReview.setStatus(Review.ReviewStatus.PUBLISHED);
        mockReview.setHelpfulCount(5);
    }

    @Test
    @DisplayName("GIVEN existing reviews WHEN getAllReviews() THEN return all reviews mapped to response")
    void shouldReturnAllReviews() {
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(userRepository.findById("usr-200")).thenReturn(Optional.of(mockUser));
        when(restaurantRepository.findById("resto-100")).thenReturn(Optional.of(mockRestaurant));

        List<ReviewResponse> result = reviewService.getAllReviews();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Excellente expérience");
        assertThat(result.get(0).getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("GIVEN valid request WHEN createReview() THEN save and publish review")
    void shouldCreateReviewSuccessfully() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5.0);
        request.setTitle("Un délice absolu");
        request.setComment("Service rapide et plats généreux.");

        when(restaurantRepository.findById("resto-100")).thenReturn(Optional.of(mockRestaurant));
        when(userRepository.findById("usr-200")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("rev-new-1");
            return r;
        });

        ReviewResponse response = reviewService.createReview("resto-100", "usr-200", request);

        assertThat(response.getId()).isEqualTo("rev-new-1");
        assertThat(response.getRating()).isEqualTo(5.0);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("GIVEN non-existent restaurant WHEN createReview() THEN throw ResourceNotFoundException")
    void shouldThrowWhenRestaurantNotFoundOnReviewCreation() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(4.0);

        when(restaurantRepository.findById("invalid-resto")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview("invalid-resto", "usr-200", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Restaurant not found");
    }

    @Test
    @DisplayName("GIVEN valid reviewId WHEN markHelpful() THEN increment helpfulCount")
    void shouldIncrementHelpfulCount() {
        when(reviewRepository.findById("rev-300")).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);

        reviewService.markHelpful("rev-300", "usr-another");

        assertThat(mockReview.getHelpfulCount()).isEqualTo(6);
        verify(reviewRepository, times(1)).save(mockReview);
    }
}