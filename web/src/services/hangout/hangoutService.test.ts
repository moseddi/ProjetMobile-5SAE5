import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { HangOut } from '../../types';

vi.mock('../api/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from '../api/apiClient';
import { hangOutService } from './hangoutService';

const getMock = apiClient.get as unknown as ReturnType<typeof vi.fn>;
const postMock = apiClient.post as unknown as ReturnType<typeof vi.fn>;
const deleteMock = apiClient.delete as unknown as ReturnType<typeof vi.fn>;

const sampleHangout = { id: 'h1', title: 'Friday Dinner' } as HangOut;

describe('hangOutService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getAll calls GET /hangouts with the upcoming flag', async () => {
    getMock.mockResolvedValue([sampleHangout]);

    const result = await hangOutService.getAll(true);

    expect(apiClient.get).toHaveBeenCalledWith('/hangouts', { upcoming: true });
    expect(result).toEqual([sampleHangout]);
  });

  it('getById calls GET /hangouts/:id', async () => {
    getMock.mockResolvedValue(sampleHangout);

    const result = await hangOutService.getById('h1');

    expect(apiClient.get).toHaveBeenCalledWith('/hangouts/h1');
    expect(result).toEqual(sampleHangout);
  });

  it('getByCreator calls GET /hangouts/creator/:userId', async () => {
    getMock.mockResolvedValue([]);

    await hangOutService.getByCreator('u1');

    expect(apiClient.get).toHaveBeenCalledWith('/hangouts/creator/u1');
  });

  it('getJoined calls GET /hangouts/joined/:userId', async () => {
    getMock.mockResolvedValue([]);

    await hangOutService.getJoined('u1');

    expect(apiClient.get).toHaveBeenCalledWith('/hangouts/joined/u1');
  });

  it('create posts the body with the userId query param', async () => {
    postMock.mockResolvedValue(sampleHangout);
    const body = {
      restaurantId: 'r1',
      title: 'Friday Dinner',
      plannedDateTime: '2999-01-01T19:30',
      maxParticipants: 4,
    };

    await hangOutService.create('u1', body);

    expect(apiClient.post).toHaveBeenCalledWith('/hangouts', body, { userId: 'u1' });
  });

  it('join posts to /hangouts/:id/join with the userId param', async () => {
    postMock.mockResolvedValue(sampleHangout);

    await hangOutService.join('h1', 'u2');

    expect(apiClient.post).toHaveBeenCalledWith('/hangouts/h1/join', null, { userId: 'u2' });
  });

  it('leave posts to /hangouts/:id/leave with the userId param', async () => {
    postMock.mockResolvedValue(sampleHangout);

    await hangOutService.leave('h1', 'u2');

    expect(apiClient.post).toHaveBeenCalledWith('/hangouts/h1/leave', null, { userId: 'u2' });
  });

  it('cancel calls DELETE /hangouts/:id with the userId in the query string', async () => {
    deleteMock.mockResolvedValue(undefined);

    await hangOutService.cancel('h1', 'u1');

    expect(apiClient.delete).toHaveBeenCalledWith('/hangouts/h1?userId=u1');
  });
});
