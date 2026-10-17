import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { gameService } from '../../../services/loyalty/gameService';
import { GameResultResponse } from '../../../types';
import LoyaltyNav from '../../../components/loyalty/LoyaltyNav';
import '../LoyaltyPage.css';
import './GamePlay.css';

interface Card {
  index: number;
  symbol: string;
  flipped: boolean;
  matched: boolean;
}

const MemoryGame = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [cards, setCards] = useState<Card[]>([]);
  const [moves, setMoves] = useState(0);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<GameResultResponse | null>(null);
  const startedAt = useRef<number>(Date.now());
  const flippedIndexes = useRef<number[]>([]);
  const busy = useRef(false);

  useEffect(() => {
    if (!user?.id) return;
    gameService.startMemory(user.id)
      .then((res) => {
        setSessionId(res.sessionId);
        setCards(res.symbols.map((symbol, index) => ({ index, symbol, flipped: false, matched: false })));
        startedAt.current = Date.now();
      })
      .catch(() => navigate('/loyalty/games'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const handleFlip = (index: number) => {
    if (busy.current) return;
    const card = cards[index];
    if (card.flipped || card.matched) return;

    const nextCards = cards.map((c) => (c.index === index ? { ...c, flipped: true } : c));
    setCards(nextCards);
    flippedIndexes.current.push(index);

    if (flippedIndexes.current.length === 2) {
      busy.current = true;
      setMoves((m) => m + 1);
      const [a, b] = flippedIndexes.current;
      const match = nextCards[a].symbol === nextCards[b].symbol;

      setTimeout(() => {
        setCards((prev) =>
          prev.map((c) => {
            if (c.index === a || c.index === b) {
              return match ? { ...c, matched: true } : { ...c, flipped: false };
            }
            return c;
          })
        );
        flippedIndexes.current = [];
        busy.current = false;
      }, 700);
    }
  };

  useEffect(() => {
    if (cards.length > 0 && cards.every((c) => c.matched) && sessionId && !result && !submitting) {
      submit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cards]);

  const submit = async () => {
    if (!user?.id || !sessionId) return;
    setSubmitting(true);
    try {
      const durationMs = Date.now() - startedAt.current;
      const res = await gameService.submitMemory(user.id, sessionId, moves, durationMs);
      setResult(res);
    } catch {
      /* handled by apiClient */
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="loading"><div className="spinner"></div></div>
      </div>
    );
  }

  if (result) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="container game-result-page">
          <h1>{result.message}</h1>
          <p className="game-result-points">+{result.pointsEarned} points — new balance: {result.newBalance}</p>
          <button className="btn btn-primary" onClick={() => navigate('/loyalty/games')}>Back to games</button>
        </div>
      </div>
    );
  }

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container memory-page">
        <div className="memory-header">
          <h2>Memory Match</h2>
          <span>Moves: {moves}</span>
        </div>
        <div className="memory-grid">
          {cards.map((card) => (
            <button
              key={card.index}
              className={`memory-card ${card.flipped || card.matched ? 'flipped' : ''} ${card.matched ? 'matched' : ''}`}
              onClick={() => handleFlip(card.index)}
            >
              <span className="memory-card-face">{card.flipped || card.matched ? card.symbol : '?'}</span>
            </button>
          ))}
        </div>
        {submitting && <p className="memory-submitting">Saving your result…</p>}
      </div>
    </div>
  );
};

export default MemoryGame;
