import { apiClient } from '../api/apiClient';
import { HangOut, HangOutRequest } from '../../types';

class HangOutService {
  async getAll(upcoming = false): Promise<HangOut[]> {
    return apiClient.get<HangOut[]>('/hangouts', { upcoming });
  }

  async getById(id: string): Promise<HangOut> {
    return apiClient.get<HangOut>(`/hangouts/${id}`);
  }

  async getByCreator(userId: string): Promise<HangOut[]> {
    return apiClient.get<HangOut[]>(`/hangouts/creator/${userId}`);
  }

  async getJoined(userId: string): Promise<HangOut[]> {
    return apiClient.get<HangOut[]>(`/hangouts/joined/${userId}`);
  }

  async create(userId: string, data: HangOutRequest): Promise<HangOut> {
    return apiClient.post<HangOut>('/hangouts', data, { userId });
  }

  async join(id: string, userId: string): Promise<HangOut> {
    return apiClient.post<HangOut>(`/hangouts/${id}/join`, null, { userId });
  }

  async leave(id: string, userId: string): Promise<HangOut> {
    return apiClient.post<HangOut>(`/hangouts/${id}/leave`, null, { userId });
  }

  async cancel(id: string, userId: string): Promise<void> {
    return apiClient.delete<void>(`/hangouts/${id}?userId=${userId}`);
  }
}

export const hangOutService = new HangOutService();
