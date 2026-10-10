import { apiClient } from '../api/apiClient';
import type { Event, GenerateDescriptionRequest, GeneratedDescription } from '../../types';

export interface RestaurantOption {
  id: string;
  name: string;
}

export const eventService = {
  getAll: async (): Promise<Event[]> => {
    const data = await apiClient.get<Event[]>('/events');
    return Array.isArray(data) ? data : [];
  },

  getRestaurants: async (): Promise<RestaurantOption[]> => {
    const data = await apiClient.get<RestaurantOption[]>('/restaurants');
    return Array.isArray(data) ? data : [];
  },

  create: (event: Partial<Event>): Promise<Event> =>
    apiClient.post<Event>('/events', event),

  // Le timeout de 30 s est déjà défini globalement dans apiClient
  generateDescription: (data: GenerateDescriptionRequest): Promise<GeneratedDescription> =>
    apiClient.post<GeneratedDescription>('/events/generate-description', data),
};