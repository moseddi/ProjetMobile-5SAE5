import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { eventService } from '../services/event/eventService';
import type { RagResponse } from '../types';
import './ReviewRagSearch.css';

const EXAMPLES = [
  'Une soirée romantique et calme',
  'Ambiance animée avec musique live',
  'Une sortie en famille avec des enfants',
  'Excellents fruits de mer',
];

const DEFAULT_IMAGE =
  'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=80';

export default function ReviewRagSearch() {
  const { user, isAuthenticated } = useAuth();
  const [query, setQuery] = useState('');
  const [data, setData] = useState<RagResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const search = async (q: string) => {
    setLoading(true);
    try {
      const res = await eventService.ragRecommend(q.trim(), user?.id);
      setData(res);
    } catch {
      // erreur déjà notifiée par l'intercepteur d'apiClient
    } finally {
      setLoading(false);
    }
  };

  const modeLabel = (mode: RagResponse['mode']) => {
    if (mode === 'PROFILE') return 'Selon vos goûts';
    if (mode === 'DEFAULT') return 'Les expériences les mieux notées';
    return 'Résultats pour votre demande';
  };

  return (
    <div className="rag">
      <h2>🔎 Trouvez un événement à partir des avis clients</h2>
      <p className="rag__intro">
        Décrivez ce que vous cherchez. Notre IA parcourt les avis réels des clients et
        justifie ses suggestions en s'appuyant sur eux.
      </p>

      <form
        className="rag__form"
        onSubmit={(e) => {
          e.preventDefault();
          search(query);
        }}
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Ex : un endroit paisible pour dîner à deux"
          maxLength={300}
        />
        <button type="submit" className="btn btn-primary" disabled={loading || !query.trim()}>
          {loading ? 'Recherche…' : '✨ Trouver avec l\'IA'}
        </button>
        {isAuthenticated && (
          <button
            type="button"
            className="btn"
            disabled={loading}
            onClick={() => {
              setQuery('');
              search('');
            }}
          >
            Suggérer selon mes goûts
          </button>
        )}
      </form>

      <div className="rag__chips">
        {EXAMPLES.map((ex) => (
          <button
            key={ex}
            type="button"
            className="rag__chip"
            disabled={loading}
            onClick={() => {
              setQuery(ex);
              search(ex);
            }}
          >
            {ex}
          </button>
        ))}
      </div>

      {loading && <p className="rag__hint">Lecture des avis en cours… (quelques secondes)</p>}

      {!loading && data && data.items.length === 0 && (
        <p className="rag__hint">{data.message ?? 'Aucun résultat.'}</p>
      )}

      {!loading && data && data.items.length > 0 && (
        <>
          <p className="rag__mode">
            {modeLabel(data.mode)} — <em>« {data.queryUsed} »</em>
          </p>
          {!data.generated && (
            <p className="rag__hint">
              Explication IA indisponible pour le moment : voici l'avis le plus pertinent.
            </p>
          )}

          <div className="rag__grid">
            {data.items.map((item) => (
              <div key={item.event.id} className="rag__card">
                <img
                  src={item.event.image || DEFAULT_IMAGE}
                  alt={item.event.title}
                  onError={(e) => {
                    e.currentTarget.src = DEFAULT_IMAGE;
                  }}
                />
                <div className="rag__body">
                  <div className="rag__badges">
                    <span className="rag__match">{item.relevancePercent}% pertinent</span>
                    <span className="rag__rating">★ {item.avgRating.toFixed(1)}</span>
                  </div>
                  <h3>{item.event.title}</h3>
                  <p className="rag__restaurant">{item.restaurantName}</p>
                  <p className="rag__date">
                    {new Date(item.event.eventDate).toLocaleDateString('fr-FR', {
                      weekday: 'short',
                      day: 'numeric',
                      month: 'long',
                    })}{' '}
                    · {item.event.price} €
                  </p>

                  <p className="rag__explanation">✨ {item.explanation}</p>

                  <details className="rag__sources">
                    <summary>Avis utilisés ({item.sources.length})</summary>
                    {item.sources.map((s) => (
                      <blockquote key={s.label}>
                        <strong>
                          {s.label} · ★ {s.rating.toFixed(0)}
                        </strong>
                        <br />« {s.text} »
                      </blockquote>
                    ))}
                  </details>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}