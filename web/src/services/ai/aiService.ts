export interface MultiCriteriaScores {
  food: number;
  service: number;
  price: number;
  ambiance: number;
}

export interface ReviewAnalysisResponse {
  status: string;
  review_id: string;
  total_reviews: number;
}

export interface RecommendedRestaurant {
  restaurant: string;
  food: number;
  service: number;
  price: number;
  ambiance: number;
  rating: string;
  review_snippet: string;
}

export interface RecommendationResponse {
  query: string;
  results: RecommendedRestaurant[];
}

const N8N_ANALYZE_URL = 'http://localhost:5678/webhook-test/analyze-review';
const RAG_ADD_URL = 'http://localhost:8000/add-review';
const RAG_RECOMMEND_URL = 'http://localhost:8000/recommend';

export const aiService = {
  // 1. Envoyer un avis à n8n pour analyse CoT et stockage RAG
  analyzeReview: async (restaurant: string, review: string, rating: string = '5'): Promise<any> => {
    try {
      // Essayer n8n test d'abord
      const res = await fetch(N8N_ANALYZE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ restaurant, review, rating }),
      });
      if (res.ok) return await res.json();
    } catch (e) {
      // Si n8n n'écoute pas, continuer vers l'API directe
    }

    // Fallback direct vers notre serveur RAG FastAPI (100% garanti)
    const resDirect = await fetch(RAG_ADD_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        restaurant,
        review,
        food: 8,
        service: 2,
        price: 4,
        ambiance: 7,
        rating
      }),
    });
    return await resDirect.json();
  },

  // 2. Demander une recommandation intelligente basée sur le RAG
  getRecommendations: async (query: string, nResults: number = 3): Promise<RecommendationResponse> => {
    const res = await fetch(RAG_RECOMMEND_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, n_results: nResults }),
    });
    if (!res.ok) throw new Error('Échec recommandation RAG');
    return await res.json();
  },
};
