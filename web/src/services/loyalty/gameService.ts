import { apiClient } from '../api/apiClient';
import {
  QuizQuestion,
  QuizSubmitRequest,
  GameResultResponse,
  MemoryStartResponse,
} from '../../types';

class GameService {
  async getTodaysQuiz(userId: string): Promise<QuizQuestion[]> {
    return apiClient.get<QuizQuestion[]>('/loyalty/games/quiz/today', { userId });
  }

  async submitQuiz(userId: string, request: QuizSubmitRequest): Promise<GameResultResponse> {
    return apiClient.post<GameResultResponse>('/loyalty/games/quiz/submit', request, { userId });
  }

  async startMemory(userId: string): Promise<MemoryStartResponse> {
    return apiClient.post<MemoryStartResponse>('/loyalty/games/memory/start', null, { userId });
  }

  async submitMemory(userId: string, sessionId: string, moves: number, durationMs: number): Promise<GameResultResponse> {
    return apiClient.post<GameResultResponse>('/loyalty/games/memory/submit', { sessionId, moves, durationMs }, { userId });
  }

  async playScratchCard(userId: string): Promise<GameResultResponse> {
    return apiClient.post<GameResultResponse>('/loyalty/games/scratch/play', null, { userId });
  }

  async spinWheel(userId: string): Promise<GameResultResponse> {
    return apiClient.post<GameResultResponse>('/loyalty/games/wheel/spin', null, { userId });
  }
}

export const gameService = new GameService();
