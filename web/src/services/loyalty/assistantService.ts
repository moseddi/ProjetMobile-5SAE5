import { apiClient } from '../api/apiClient';
import { AssistantChatResponse } from '../../types';

class LoyaltyAssistantService {
  async chat(userId: string, message: string): Promise<AssistantChatResponse> {
    return apiClient.post<AssistantChatResponse>('/loyalty/assistant/chat', { message }, { userId });
  }
}

export const loyaltyAssistantService = new LoyaltyAssistantService();
