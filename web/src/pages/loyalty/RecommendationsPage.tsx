import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { recommendationService } from '../../services/loyalty/recommendationService';
import { RestaurantRecommendationCard, RestaurantRecommendationResponse } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import './LoyaltyPage.css';
import './RecommendationsPage.css';

type Goal = 'SPEND_NOW' | 'SAVE_FOR_PREMIUM';

const RecommendationsPage = () => {
  const { user } = useAuth();
  const [data, setData] = useState<RestaurantRecommendationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [goal, setGoal] = useState<Goal>('SPEND_NOW');

  useEffect(() => {
    if (user?.id) load(goal);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, goal]);

  const load = async (selectedGoal: Goal) => {
    setLoading(true);
    setError(false);
    try {
      const result = await recommendationService.getRestaurantRecommendations(selectedGoal);
      setData(result);
    } catch (err) {
      console.error('Failed to load restaurant recommendations:', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container-wide recommendations-page">
        <div className="recommendations-header">
          <div>
            <h1 className="section-title-loyalty">Meilleurs restaurants pour vous</h1>
            <p className="recommendations-subtitle">
              Des suggestions personnalisées selon vos points, vos préférences et vos récompenses disponibles.
            </p>
          </div>
          <div className="goal-toggle">
            <button
              className={`filter-chip ${goal === 'SPEND_NOW' ? 'active' : ''}`}
              onClick={() => setGoal('SPEND_NOW')}
            >
              Dépenser maintenant
            </button>
            <button
              className={`filter-chip ${goal === 'SAVE_FOR_PREMIUM' ? 'active' : ''}`}
              onClick={() => setGoal('SAVE_FOR_PREMIUM')}
            >
              Épargner pour mieux
            </button>
          </div>
        </div>

        {loading && (
          <div className="loading"><div className="spinner"></div></div>
        )}

        {!loading && error && (
          <div className="recommendations-error">
            <p>Impossible de charger vos recommandations pour le moment.</p>
            <button className="btn btn-primary" onClick={() => load(goal)}>Réessayer</button>
          </div>
        )}

        {!loading && !error && data && (
          <>
            {data.notice && (
              <div className="recommendations-notice">{data.notice}</div>
            )}

            {data.message && <p className="recommendations-message">{data.message}</p>}

            {data.recommendations.length === 0 ? (
              <div className="recommendations-empty">
                <p>Aucune recommandation disponible pour le moment.</p>
                <p className="recommendations-empty-hint">
                  Revenez après avoir gagné plus de points ou dès que de nouvelles récompenses seront ajoutées.
                </p>
              </div>
            ) : (
              <div className="recommendations-grid">
                {data.recommendations.map((card) => (
                  <RecommendationCardView key={`${card.restaurantId}-${card.rewardId}`} card={card} />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

const RecommendationCardView = ({ card }: { card: RestaurantRecommendationCard }) => {
  return (
    <div className={`recommendation-card ${!card.affordable ? 'locked' : ''}`}>
      {card.restaurantImage && (
        <img src={card.restaurantImage} alt={card.restaurantName} className="recommendation-card-image" />
      )}
      <div className="recommendation-card-body">
        <div className="recommendation-card-top">
          <h3>{card.restaurantName}</h3>
          {card.rating != null && <span className="recommendation-rating">★ {card.rating.toFixed(1)}</span>}
        </div>
        <div className="recommendation-card-meta">
          {card.cuisine && <span>{card.cuisine}</span>}
          {card.priceRange && <span>{card.priceRange}</span>}
        </div>

        <div className="recommendation-reward-box">
          <div className="recommendation-reward-name">{card.rewardName}</div>
          <div className="recommendation-reward-cost">{card.pointsCost} points</div>
        </div>

        {card.affordable ? (
          <p className="recommendation-status affordable">
            Vous pouvez l'échanger maintenant — il vous restera {card.pointsRemainingAfterRedemption} points.
          </p>
        ) : (
          <p className="recommendation-status locked-text">
            Il vous manque {card.pointsMissing} points pour cette récompense.
          </p>
        )}

        {card.rewardTerms && <p className="recommendation-terms">{card.rewardTerms}</p>}
        {card.rewardExpiryDate && (
          <p className="recommendation-expiry">Offre valable jusqu'au {new Date(card.rewardExpiryDate).toLocaleDateString()}</p>
        )}

        {card.reason && <p className="recommendation-reason">{card.reason}</p>}

        <div className="recommendation-actions">
          <Link to={`/restaurants/${card.restaurantId}`} className="btn btn-outline btn-sm">
            Voir le restaurant
          </Link>
          {card.affordable && (
            <Link to="/loyalty/rewards" className="btn btn-primary btn-sm">
              Voir la récompense
            </Link>
          )}
        </div>
      </div>
    </div>
  );
};

export default RecommendationsPage;
