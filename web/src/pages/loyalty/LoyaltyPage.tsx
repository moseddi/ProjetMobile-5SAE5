import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { loyaltyService } from '../../services/loyalty/loyaltyService';
import { LoyaltyDashboard } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import { iconForGame } from '../../components/loyalty/gameIcons';
import './LoyaltyPage.css';

const LoyaltyPage = () => {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState<LoyaltyDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user?.id) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const load = async () => {
    setLoading(true);
    try {
      const data = await loyaltyService.getDashboard(user!.id);
      setDashboard(data);
    } catch (error) {
      console.error('Failed to load loyalty dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !dashboard) {
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

      <section className="loyalty-hero">
        <div className="container-wide">
          <div className="loyalty-hero-content">
            <h1 className="loyalty-hero-title">
              Your <span className="accent-text">Rewards</span>
            </h1>
            <p className="loyalty-hero-subtitle">
              Earn points by playing games and dining out, then redeem exclusive rewards
            </p>
          </div>
        </div>
      </section>

      <div className="container-wide">
        <section className="loyalty-overview">
          <div className="points-card">
            <div className="points-main">
              <div className="points-number">{dashboard.points.toLocaleString()}</div>
              <div className="points-label">Available Points</div>
            </div>
            <div className="tier-badge">{dashboard.tier} Member</div>
          </div>

          <div className="progress-card">
            {dashboard.nextTier ? (
              <>
                <div className="progress-header">
                  <h3>Progress to {dashboard.nextTier}</h3>
                  <span className="progress-remaining">{dashboard.pointsToNextTier} points to go</span>
                </div>
                <div className="progress-bar-container">
                  <div className="progress-bar-fill" style={{ width: `${dashboard.tierProgressPercent}%` }}></div>
                </div>
              </>
            ) : (
              <h3>You've reached the highest tier — Platinum!</h3>
            )}
          </div>
        </section>

        <section className="recommendation-cta-section">
          <Link to="/loyalty/recommendations" className="recommendation-cta-card">
            <div className="recommendation-cta-icon">✨</div>
            <div className="recommendation-cta-text">
              <h3>Trouver les meilleurs restaurants pour moi</h3>
              <p>Découvrez où utiliser vos points grâce à des recommandations personnalisées.</p>
            </div>
            <span className="recommendation-cta-arrow">→</span>
          </Link>
        </section>

        <section className="challenges-section">
          <h2 className="section-title-loyalty">Today's Challenges</h2>
          <div className="challenges-grid">
            {dashboard.todaysChallenges.map((c) => (
              <div key={c.id} className={`challenge-card ${c.completed ? 'completed' : ''}`}>
                <div className="challenge-card-header">
                  <h4>{c.title}</h4>
                  <span className="challenge-points">+{c.pointsReward} pts</span>
                </div>
                <p>{c.description}</p>
                <div className="progress-bar-container small">
                  <div className="progress-bar-fill" style={{ width: `${Math.min(100, (c.progress / c.target) * 100)}%` }}></div>
                </div>
                <span className="challenge-progress-label">
                  {c.completed ? '✓ Completed' : `${c.progress}/${c.target}`}
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="games-section">
          <div className="section-header-row">
            <h2 className="section-title-loyalty">Games</h2>
            <Link to="/loyalty/games" className="link-see-all">See all →</Link>
          </div>
          <div className="games-grid-preview">
            {dashboard.games.map((g) => (
              <Link key={g.id} to={`/loyalty/games/${g.id}`} className="game-tile">
                <div className="game-tile-icon">{iconForGame(g.id)}</div>
                <h4>{g.name}</h4>
                <p>{g.pointsReward} points</p>
                <span className={`game-tile-status ${g.availableToday ? 'available' : 'unavailable'}`}>
                  {g.availableToday ? 'Play now' : `${g.attemptsUsedToday}/${g.dailyLimit} today`}
                </span>
              </Link>
            ))}
          </div>
        </section>

        <section className="rewards-section">
          <div className="section-header-row">
            <h2 className="section-title-loyalty">Recommended for you</h2>
            <Link to="/loyalty/rewards" className="link-see-all">See all rewards →</Link>
          </div>
          <p className="rewards-affordable-hint">
            You can currently afford <strong>{dashboard.affordableRewardsCount}</strong> reward{dashboard.affordableRewardsCount === 1 ? '' : 's'}.
          </p>
        </section>

        <section className="assistant-teaser">
          <div className="assistant-teaser-card">
            <div>
              <h3>✨ Mon Coach Fidelite</h3>
              <p>Ask the assistant what games to play or what you can redeem with your points.</p>
            </div>
            <Link to="/loyalty/assistant" className="btn btn-primary">Chat now</Link>
          </div>
        </section>
      </div>
    </div>
  );
};

export default LoyaltyPage;
