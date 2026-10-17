import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { gameService } from '../../../services/loyalty/gameService';
import { loyaltyService } from '../../../services/loyalty/loyaltyService';
import { GameResultResponse } from '../../../types';
import LoyaltyNav from '../../../components/loyalty/LoyaltyNav';
import '../LoyaltyPage.css';
import './GamePlay.css';

const SEGMENTS = [0, 5, 15, 25, 50, 75, 100, 150];

const WheelGame = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [spinning, setSpinning] = useState(false);
  const [rotation, setRotation] = useState(0);
  const [result, setResult] = useState<GameResultResponse | null>(null);
  const [checkingAvailability, setCheckingAvailability] = useState(true);
  const [availableToday, setAvailableToday] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    loyaltyService.getGames(user.id)
      .then((games) => setAvailableToday(games.find((g) => g.id === 'wheel')?.availableToday ?? true))
      .finally(() => setCheckingAvailability(false));
  }, [user?.id]);

  const spin = async () => {
    if (!user?.id || spinning) return;
    setSpinning(true);
    setResult(null);
    try {
      // The backend already decided the prize; we just animate the wheel to a
      // visually plausible stop (several full turns + a pseudo-random offset),
      // then reveal the server's real result once the animation settles.
      const res = await gameService.spinWheel(user.id);
      const extraTurns = 4 * 360;
      const randomOffset = Math.floor(Math.random() * 360);
      setRotation((r) => r + extraTurns + randomOffset);
      setTimeout(() => setResult(res), 3200);
    } catch {
      setSpinning(false);
      setAvailableToday(false);
    }
  };

  if (checkingAvailability) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="loading"><div className="spinner"></div></div>
      </div>
    );
  }

  if (!availableToday) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="container game-result-page">
          <h2>🎡 Roue quotidienne</h2>
          <p>Tu as deja utilise ton tour gratuit aujourd'hui. Reviens demain pour retenter ta chance !</p>
          <button className="btn btn-primary" onClick={() => navigate('/loyalty/games')}>Back to games</button>
        </div>
      </div>
    );
  }

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container game-result-page">
        <h2>🎡 Roue quotidienne</h2>
        <p>Un tour gratuit par jour. Tente ta chance pour gagner jusqu'a 150 points !</p>

        <div className="wheel-container">
          <div className="wheel-pointer">▼</div>
          <div
            className="wheel"
            style={{ transform: `rotate(${rotation}deg)`, transition: spinning ? 'transform 3.2s cubic-bezier(0.17, 0.67, 0.21, 1)' : 'none' }}
            onTransitionEnd={() => setSpinning(false)}
          >
            {SEGMENTS.map((value, i) => (
              <div key={i} className="wheel-segment" style={{ transform: `rotate(${(360 / SEGMENTS.length) * i}deg)` }}>
                <span>{value}</span>
              </div>
            ))}
          </div>
        </div>

        <button className="btn btn-primary" disabled={spinning} onClick={spin}>
          {spinning ? 'La roue tourne…' : 'Faire tourner la roue'}
        </button>

        {result && !spinning && (
          <div className="game-result-points" style={{ marginTop: 16 }}>
            {result.message} {result.pointsEarned > 0 && `(nouveau solde: ${result.newBalance})`}
          </div>
        )}

        <button className="btn btn-outline" style={{ marginTop: 24 }} onClick={() => navigate('/loyalty/games')}>
          Back to games
        </button>
      </div>
    </div>
  );
};

export default WheelGame;
