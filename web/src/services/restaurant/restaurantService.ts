import { apiClient } from '../api/apiClient';
import { Restaurant } from '../../types';

const API_BASE_URL = (import.meta as any).env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export function resolveRestaurantMediaUrl(path?: string): string {
  if (!path) {
    return '';
  }
  if (path.startsWith('http') || path.startsWith('data:') || path.startsWith('blob:')) {
    return path;
  }
  const origin = API_BASE_URL.replace(/\/api\/?$/, '');
  return `${origin}${path.startsWith('/') ? path : `/${path}`}`;
}

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

  async create(restaurant: Omit<Restaurant, 'id' | 'createdAt' | 'updatedAt'>): Promise<Restaurant> {
    return apiClient.post<Restaurant>('/restaurants', restaurant);
  }

  async update(id: string, restaurant: Restaurant): Promise<Restaurant> {
    return apiClient.put<Restaurant>(`/restaurants/${id}`, restaurant);
  }

  async remove(id: string): Promise<void> {
    return apiClient.delete<void>(`/restaurants/${id}`);
  }

  async uploadMenuImage(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/restaurants/menu-image`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.message || 'Failed to upload menu image');
    }

    const data = await response.json();
    return data.url as string;
  }
}

export const restaurantService = new RestaurantService();
