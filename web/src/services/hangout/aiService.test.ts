import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { AiHangoutResponse } from '../../types';

vi.mock('../api/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from '../api/apiClient';
import { aiService } from './aiService';

const postMock = apiClient.post as unknown as ReturnType<typeof vi.fn>;

const sampleResponse: AiHangoutResponse = {
  restaurantId: 'r-it',
  restaurantName: 'Bella Italia',
  title: 'Italian Night',
  description: 'Join us for a great meal',
  suggestedDateTime: '2999-01-01T19:30',
  keywords: ['Italian', 'Dinner'],
  reason: 'Matches your request',
  suggestedPartySize: 4,
  aiGenerated: true,
  provider: 'gemini-3.5-flash',
  notice: null,
};

describe('aiService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('posts the request to /ai/hangout-assistant and returns the response', async () => {
    postMock.mockResolvedValue(sampleResponse);

    const request = { prompt: 'italian dinner', partySize: 4, budget: 'MODERATE' };
    const result = await aiService.suggestHangout(request);

    expect(apiClient.post).toHaveBeenCalledTimes(1);
    expect(apiClient.post).toHaveBeenCalledWith('/ai/hangout-assistant', request);
    expect(result).toEqual(sampleResponse);
  });

  it('propagates errors from the API client', async () => {
    postMock.mockRejectedValue(new Error('network down'));

    await expect(aiService.suggestHangout({ prompt: 'x' })).rejects.toThrow('network down');
  });
});
