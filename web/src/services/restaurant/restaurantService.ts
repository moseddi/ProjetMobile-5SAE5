import { apiClient } from '../api/apiClient';
import { Restaurant } from '../../types';

class RestaurantService {
  async getAll(): Promise<Restaurant[]> {
    return apiClient.get<Restaurant[]>('/restaurants');
  }

  async getById(id: string): Promise<Restaurant> {
    return apiClient.get<Restaurant>(`/restaurants/${id}`);
  }

  async getByCuisine(cuisine: string): Promise<Restaurant[]> {
    return apiClient.get<Restaurant[]>(`/restaurants/cuisine/${cuisine}`);
  }
}

export const restaurantService = new RestaurantService();
