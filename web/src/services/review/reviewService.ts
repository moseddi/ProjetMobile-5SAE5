import { apiClient } from '../api/apiClient';
import { Review, ReviewRequest } from '../../types';

class ReviewService {
  async getAll(): Promise<Review[]> {
    return apiClient.get<Review[]>('/reviews');
  }

  async getByRestaurant(restaurantId: string): Promise<Review[]> {
    return apiClient.get<Review[]>(`/reviews/restaurant/${restaurantId}`);
  }

  async create(restaurantId: string, userId: string, data: ReviewRequest): Promise<Review> {
    return apiClient.post<Review>(`/reviews/restaurant/${restaurantId}`, data, { userId });
  }

  async markHelpful(reviewId: string, userId: string): Promise<void> {
    return apiClient.put<void>(`/reviews/${reviewId}/helpful?userId=${userId}`, null);
  }

  async delete(reviewId: string, userId: string): Promise<void> {
    return apiClient.delete<void>(`/reviews/${reviewId}?userId=${userId}`);
  }
}

export const reviewService = new ReviewService();
