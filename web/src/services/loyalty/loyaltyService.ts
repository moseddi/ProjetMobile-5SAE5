import { apiClient } from '../api/apiClient';
import {
  LoyaltyDashboard,
  LoyaltyPointTransaction,
  LoyaltyReward,
  LoyaltyRedemption,
  LoyaltyChallenge,
  LoyaltyGame,
} from '../../types';

class LoyaltyService {
  async getDashboard(userId: string): Promise<LoyaltyDashboard> {
    return apiClient.get<LoyaltyDashboard>('/loyalty/dashboard', { userId });
  }

  async getTransactions(userId: string): Promise<LoyaltyPointTransaction[]> {
    return apiClient.get<LoyaltyPointTransaction[]>('/loyalty/transactions', { userId });
  }

  async getRewards(userId: string): Promise<LoyaltyReward[]> {
    return apiClient.get<LoyaltyReward[]>('/loyalty/rewards', { userId });
  }

  async redeemReward(userId: string, rewardId: string): Promise<LoyaltyRedemption> {
    return apiClient.post<LoyaltyRedemption>(`/loyalty/rewards/${rewardId}/redeem`, null, { userId });
  }

  async getRedemptions(userId: string): Promise<LoyaltyRedemption[]> {
    return apiClient.get<LoyaltyRedemption[]>('/loyalty/redemptions', { userId });
  }

  async getTodaysChallenges(userId: string): Promise<LoyaltyChallenge[]> {
    return apiClient.get<LoyaltyChallenge[]>('/loyalty/challenges', { userId });
  }

  async getGames(userId: string): Promise<LoyaltyGame[]> {
    return apiClient.get<LoyaltyGame[]>('/loyalty/games', { userId });
  }
}

export const loyaltyService = new LoyaltyService();
