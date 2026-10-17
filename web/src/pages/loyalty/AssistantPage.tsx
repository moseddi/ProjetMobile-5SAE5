import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { loyaltyAssistantService } from '../../services/loyalty/assistantService';
import { AssistantChatResponse, AssistantRecommendation } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import './LoyaltyPage.css';
import './AssistantPage.css';

interface ChatEntry {
  role: 'user' | 'assistant';
  text: string;
  recommendations?: AssistantRecommendation[];
  followUpQuestion?: string | null;
  aiGenerated?: boolean;
  notice?: string | null;
}

const SUGGESTED_PROMPTS = [
  'Quels jeux puis-je jouer aujourd\'hui ?',
  'Que puis-je obtenir avec mes points ?',
  'Comment gagner plus de points ?',
  'Quelle récompense me correspond le mieux ?',
];

const AssistantPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [entries, setEntries] = useState<ChatEntry[]>([
    { role: 'assistant', text: 'Bonjour ! Je suis Mon Coach Fidelite. Demande-moi quels jeux jouer ou quelles recompenses t\'attendent.' },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const send = async (text?: string) => {
    const message = (text ?? input).trim();
    if (!message || !user?.id || sending) return;

    setEntries((prev) => [...prev, { role: 'user', text: message }]);
    setInput('');
    setSending(true);
    try {
      const res: AssistantChatResponse = await loyaltyAssistantService.chat(user.id, message);
      setEntries((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: res.message,
          recommendations: res.recommendations,
          followUpQuestion: res.followUpQuestion,
          aiGenerated: res.aiGenerated,
          notice: res.notice,
        },
      ]);
    } catch {
      setEntries((prev) => [...prev, { role: 'assistant', text: "Desole, je n'ai pas pu repondre. Reessaie dans un instant." }]);
    } finally {
      setSending(false);
      setTimeout(() => listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' }), 50);
    }
  };

  const goToRecommendation = (rec: AssistantRecommendation) => {
    if (rec.type === 'GAME') {
      navigate(`/loyalty/games/${rec.id}`);
    } else {
      navigate('/loyalty/rewards');
    }
  };

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container assistant-page">
        <div className="assistant-chat-window" ref={listRef}>
          {entries.map((entry, i) => (
            <div key={i} className={`assistant-bubble ${entry.role}`}>
              <p>{entry.text}</p>

              {entry.recommendations && entry.recommendations.length > 0 && (
                <div className="assistant-recs">
                  {entry.recommendations.map((rec, j) => (
                    <button key={j} className="assistant-rec-card" onClick={() => goToRecommendation(rec)}>
                      <span className="assistant-rec-type">{rec.type === 'GAME' ? '🎮' : '🎁'}</span>
                      <div>
                        <strong>{rec.title}</strong>
                        <p>{rec.reason}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}

              {entry.followUpQuestion && <p className="assistant-followup">{entry.followUpQuestion}</p>}

              {entry.notice && <p className="assistant-notice">ℹ️ {entry.notice}</p>}
              {entry.role === 'assistant' && entry.aiGenerated === false && !entry.notice && (
                <p className="assistant-notice">ℹ️ Reponse basee sur des regles (IA indisponible).</p>
              )}
            </div>
          ))}
          {sending && <div className="assistant-bubble assistant typing">…</div>}
        </div>

        <div className="assistant-suggestions">
          {SUGGESTED_PROMPTS.map((p) => (
            <button key={p} className="suggestion-chip" onClick={() => send(p)} disabled={sending}>
              {p}
            </button>
          ))}
        </div>

        <form
          className="assistant-input-row"
          onSubmit={(e) => {
            e.preventDefault();
            send();
          }}
        >
          <input
            className="form-input"
            placeholder="Pose ta question sur tes points, tes jeux ou tes recompenses…"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={sending}
          />
          <button type="submit" className="btn btn-primary" disabled={sending || !input.trim()}>
            Envoyer
          </button>
        </form>
      </div>
    </div>
  );
};

export default AssistantPage;
