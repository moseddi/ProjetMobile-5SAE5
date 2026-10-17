import { apiClient } from '../api/apiClient';
import { AvailabilityResult, Reservation, ReservationRequest } from '../../types';

class ReservationService {
  async getUserReservations(userId: string): Promise<Reservation[]> {
    return apiClient.get<Reservation[]>(`/users/${userId}/reservations`);
  }

  async getById(id: string): Promise<Reservation> {
    return apiClient.get<Reservation>(`/reservations/${id}`);
  }

  // The backend identifies the user from the JWT sent by apiClient
  async create(data: ReservationRequest): Promise<Reservation> {
    return apiClient.post<Reservation>('/reservations', data);
  }

  async update(id: string, data: ReservationRequest): Promise<Reservation> {
    return apiClient.put<Reservation>(`/reservations/${id}`, data);
  }

  async cancel(id: string): Promise<void> {
    return apiClient.delete<void>(`/reservations/${id}`);
  }

  async checkAvailability(restaurantId: string, dateTime: string, partySize: number): Promise<AvailabilityResult> {
    return apiClient.get<AvailabilityResult>('/reservations/availability', { restaurantId, dateTime, partySize });
  }
}

export const reservationService = new ReservationService();
