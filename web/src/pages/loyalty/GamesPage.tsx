import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { loyaltyService } from '../../services/loyalty/loyaltyService';
import { LoyaltyGame } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import { iconForGame } from '../../components/loyalty/gameIcons';
import './LoyaltyPage.css';
import './GamesPage.css';

const GamesPage = () => {
  const { user } = useAuth();
  const [games, setGames] = useState<LoyaltyGame[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loyaltyService.getGames(user.id).then(setGames).finally(() => setLoading(false));
    }
  }, [user?.id]);

  if (loading) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="loading"><div className="spinner"></div></div>
      </div>
    );
  }

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container-wide games-catalog-page">
        <h1 className="section-title-loyalty">Games</h1>
        <p className="games-catalog-subtitle">Play for free, earn points, come back tomorrow for more.</p>

        <div className="games-catalog-grid">
          {games.map((g) => (
            <div key={g.id} className="game-catalog-card">
              <div className="game-catalog-icon">{iconForGame(g.id)}</div>
              <div className="game-catalog-body">
                <div className="game-catalog-header">
                  <h3>{g.name}</h3>
                  <span className="difficulty-badge">{g.difficulty}</span>
                </div>
                <p className="game-catalog-description">{g.description}</p>
                <p className="game-catalog-rules">{g.rules}</p>
                <div className="game-catalog-meta">
                  <span>⏱ ~{g.estimatedDurationMinutes} min</span>
                  <span>🏆 {g.pointsReward} pts</span>
                  <span>{g.attemptsUsedToday}/{g.dailyLimit} today</span>
                </div>
                <Link
                  to={g.availableToday ? `/loyalty/games/${g.id}` : '#'}
                  className={`btn btn-primary btn-block ${!g.availableToday ? 'btn-disabled' : ''}`}
                  aria-disabled={!g.availableToday}
                  onClick={(e) => !g.availableToday && e.preventDefault()}
                >
                  {g.availableToday ? 'Play now' : 'Come back tomorrow'}
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default GamesPage;
