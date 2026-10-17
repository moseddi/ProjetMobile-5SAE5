import { apiClient } from '../api/apiClient';
import { RestaurantRecommendationResponse } from '../../types';

class RecommendationService {
  async getRestaurantRecommendations(goal?: string, message?: string): Promise<RestaurantRecommendationResponse> {
    return apiClient.get<RestaurantRecommendationResponse>('/loyalty/recommendations/restaurants', {
      goal,
      message,
    });
  }
}

export const recommendationService = new RecommendationService();
