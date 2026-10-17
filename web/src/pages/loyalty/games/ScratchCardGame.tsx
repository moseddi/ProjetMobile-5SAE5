import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { gameService } from '../../../services/loyalty/gameService';
import { loyaltyService } from '../../../services/loyalty/loyaltyService';
import { GameResultResponse } from '../../../types';
import LoyaltyNav from '../../../components/loyalty/LoyaltyNav';
import '../LoyaltyPage.css';
import './GamePlay.css';

const ScratchCardGame = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [result, setResult] = useState<GameResultResponse | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [checkingAvailability, setCheckingAvailability] = useState(true);
  const [availableToday, setAvailableToday] = useState(true);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const scratchedPixels = useRef(0);

  useEffect(() => {
    if (!user?.id) return;
    loyaltyService.getGames(user.id)
      .then((games) => setAvailableToday(games.find((g) => g.id === 'scratch')?.availableToday ?? true))
      .finally(() => setCheckingAvailability(false));
  }, [user?.id]);

  const start = async () => {
    if (!user?.id || loading) return;
    setLoading(true);
    try {
      // The backend has already decided the outcome before any scratching happens —
      // the card interaction is purely a reveal animation over a known result.
      const res = await gameService.playScratchCard(user.id);
      setResult(res);
      setupCanvas();
    } catch {
      setAvailableToday(false);
    } finally {
      setLoading(false);
    }
  };

  const setupCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.fillStyle = '#c0c0c0';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#9ca3af';
    ctx.font = 'bold 16px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('Scratch here ✨', canvas.width / 2, canvas.height / 2);
  };

  const scratch = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (revealed) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const rect = canvas.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    const x = clientX - rect.left;
    const y = clientY - rect.top;

    ctx.globalCompositeOperation = 'destination-out';
    ctx.beginPath();
    ctx.arc(x, y, 18, 0, Math.PI * 2);
    ctx.fill();

    scratchedPixels.current += 1;
    if (scratchedPixels.current > 25) {
      setRevealed(true);
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

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container game-result-page">
        <h2>🎫 Carte a gratter</h2>

        {!availableToday && !result ? (
          <>
            <p>Tu as deja utilise ta carte gratuite aujourd'hui. Reviens demain pour une nouvelle carte !</p>
          </>
        ) : !result ? (
          <>
            <p>Gratte une carte gratuite par jour pour tenter de gagner des points.</p>
            <button className="btn btn-primary" disabled={loading} onClick={start}>
              {loading ? 'Preparation…' : 'Obtenir ma carte'}
            </button>
          </>
        ) : (
          <div className="scratch-card-wrapper">
            <div className="scratch-card-prize">{result.pointsEarned > 0 ? `+${result.pointsEarned} points` : 'Pas de chance'}</div>
            <canvas
              ref={canvasRef}
              width={280}
              height={160}
              className="scratch-canvas"
              onMouseMove={(e) => e.buttons === 1 && scratch(e)}
              onMouseDown={scratch}
              onTouchMove={scratch}
            />
            {revealed && (
              <div className="game-result-points" style={{ marginTop: 16 }}>
                {result.message} {result.pointsEarned > 0 && `(nouveau solde: ${result.newBalance})`}
              </div>
            )}
          </div>
        )}

        <button className="btn btn-outline" style={{ marginTop: 24 }} onClick={() => navigate('/loyalty/games')}>
          Back to games
        </button>
      </div>
    </div>
  );
};

export default ScratchCardGame;
