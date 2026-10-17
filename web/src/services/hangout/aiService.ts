import { apiClient } from '../api/apiClient';
import { AiHangoutRequest, AiHangoutResponse } from '../../types';

class AiService {
  async suggestHangout(data: AiHangoutRequest): Promise<AiHangoutResponse> {
    return apiClient.post<AiHangoutResponse>('/ai/hangout-assistant', data);
  }
}

export const aiService = new AiService();
