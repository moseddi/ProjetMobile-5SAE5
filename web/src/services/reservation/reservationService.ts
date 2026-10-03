import { apiClient } from '../api/apiClient';
import { Reservation, ReservationRequest } from '../../types';

class ReservationService {
  async getUserReservations(userId: string): Promise<Reservation[]> {
    return apiClient.get<Reservation[]>(`/users/${userId}/reservations`);
  }

  async getById(id: string): Promise<Reservation> {
    return apiClient.get<Reservation>(`/reservations/${id}`);
  }

  async create(userId: string, data: ReservationRequest): Promise<Reservation> {
    return apiClient.post<Reservation>('/reservations', data, { userId });
  }

  async cancel(id: string): Promise<void> {
    return apiClient.delete<void>(`/reservations/${id}`);
  }
}

export const reservationService = new ReservationService();
