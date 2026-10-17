import axios, { AxiosError } from 'axios';
import { ChatResponse, ChatTurn, EventBookingProposal, ReservationProposal } from '../../types';

const API_BASE_URL = (import.meta as any).env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Dedicated client: errors are shown inside the chat window instead of the global toasts of apiClient
const client = axios.create({
  baseURL: `${API_BASE_URL}/chatbot/reservation`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export class ChatbotError extends Error {}

const toChatbotError = (error: unknown): ChatbotError => {
  const axiosError = error as AxiosError<{ message?: string }>;
  const status = axiosError.response?.status;
  const serverMessage = axiosError.response?.data?.message;
  if (status === 401) return new ChatbotError('Votre session a expiré. Veuillez vous reconnecter.');
  if (status === 429) return new ChatbotError(serverMessage || 'Trop de messages. Patientez une minute.');
  if (status === 400) return new ChatbotError(serverMessage || 'Message invalide (500 caractères maximum).');
  if (status === 503) return new ChatbotError(serverMessage || "L'assistant est momentanément indisponible.");
  if (!axiosError.response) return new ChatbotError('Impossible de joindre le serveur. Vérifiez votre connexion.');
  return new ChatbotError(serverMessage || 'Une erreur est survenue. Réessayez.');
};

class ChatbotService {
  async sendMessage(
    message: string,
    history: ChatTurn[],
    restaurantId?: string | null,
    eventId?: string | null
  ): Promise<ChatResponse> {
    try {
      const { data } = await client.post<ChatResponse>('/message', { message, history, restaurantId, eventId });
      return data;
    } catch (error) {
      throw toChatbotError(error);
    }
  }

  async confirm(proposal: ReservationProposal): Promise<ChatResponse> {
    try {
      const { data } = await client.post<ChatResponse>('/confirm', proposal);
      return data;
    } catch (error) {
      throw toChatbotError(error);
    }
  }

  async confirmEvent(proposal: EventBookingProposal): Promise<ChatResponse> {
    try {
      const { data } = await client.post<ChatResponse>('/confirm-event', proposal);
      return data;
    } catch (error) {
      throw toChatbotError(error);
    }
  }
}

export const chatbotService = new ChatbotService();
