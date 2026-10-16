const API_BASE_URL = (import.meta as any).env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export interface DishMatch {
  id?: string;
  restaurantId?: string;
  restaurantName?: string;
  dishName?: string;
  text?: string;
  price?: number;
}

export interface PhotoSearchResponse {
  description: string;
  answer: string;
  matches: DishMatch[];
}

async function parseError(response: Response): Promise<string> {
  const body = await response.json().catch(() => ({}));
  return body.message || body.error || `Request failed (${response.status})`;
}

class AssistantApi {
  async chat(message: string): Promise<string> {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/assistant/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message }),
    });

    if (!response.ok) {
      throw new Error(await parseError(response));
    }

    const data = await response.json();
    return data.answer as string;
  }

  async searchByPhoto(file: File): Promise<PhotoSearchResponse> {
    if (file.size > 5 * 1024 * 1024) {
      throw new Error('Image trop lourde (5 Mo max)');
    }

    const formData = new FormData();
    formData.append('image', file);

    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/assistant/photo`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    });

    if (!response.ok) {
      throw new Error(await parseError(response));
    }

    const data = await response.json();
    return {
      description: data.description || '',
      answer: data.answer || '',
      matches: Array.isArray(data.matches) ? data.matches : [],
    };
  }
}

export const assistantService = new AssistantApi();
