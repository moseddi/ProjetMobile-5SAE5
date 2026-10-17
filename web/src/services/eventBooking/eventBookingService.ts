import { apiClient } from '../api/apiClient';
import { EventBooking } from '../../types';

class EventBookingService {
  async getUserBookings(userId: string): Promise<EventBooking[]> {
    return apiClient.get<EventBooking[]>(`/users/${userId}/event-bookings`);
  }

  // The backend identifies the user from the JWT sent by apiClient
  async book(eventId: string, partySize: number): Promise<EventBooking> {
    return apiClient.post<EventBooking>('/event-bookings', { eventId, partySize });
  }

  async cancel(id: string): Promise<void> {
    return apiClient.delete<void>(`/event-bookings/${id}`);
  }
}

export const eventBookingService = new EventBookingService();
