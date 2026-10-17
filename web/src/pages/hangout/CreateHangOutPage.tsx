import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { aiService } from '../../services/hangout/aiService';
import { hangOutService } from '../../services/hangout/hangoutService';
import { restaurantService } from '../../services/restaurant/restaurantService';
import { AiHangoutResponse, Restaurant } from '../../types';
import './CreateHangOutPage.css';

const BUDGETS = ['', 'BUDGET', 'MODERATE', 'EXPENSIVE', 'LUXURY'];

const CreateHangOutPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [askingAi, setAskingAi] = useState(false);
  const [saving, setSaving] = useState(false);
  const [aiResult, setAiResult] = useState<AiHangoutResponse | null>(null);

  // AI assistant inputs
  const [prompt, setPrompt] = useState('');
  const [partySize, setPartySize] = useState(4);
  const [budget, setBudget] = useState('');

  // Hangout form (pre-filled by AI)
  const [form, setForm] = useState({
    restaurantId: '',
    title: '',
    description: '',
    plannedDateTime: '',
    maxParticipants: 6,
  });

  useEffect(() => {
    restaurantService
      .getAll()
      .then(setRestaurants)
      .catch(() => toast.error('Could not load restaurants'));
  }, []);

  const askAi = async () => {
    if (!prompt.trim()) {
      toast.info('Describe your hangout idea first');
      return;
    }
    setAskingAi(true);
    try {
      const result = await aiService.suggestHangout({
        prompt: prompt.trim(),
        partySize,
        budget: budget || undefined,
      });
      setAiResult(result);
      setForm({
        restaurantId: result.restaurantId || '',
        title: result.title || '',
        description: result.description || '',
        plannedDateTime: (result.suggestedDateTime || '').slice(0, 16),
        maxParticipants: result.suggestedPartySize || partySize,
      });
      if (result.aiGenerated) {
        toast.success('AI assistant prepared your hangout!');
      } else {
        toast.warn('AI unavailable — a smart suggestion was generated instead.');
      }
    } catch {
      /* handled by apiClient */
    } finally {
      setAskingAi(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) {
      toast.error('You must be signed in');
      return;
    }
    if (!form.restaurantId || !form.title || !form.plannedDateTime) {
      toast.info('Please pick a restaurant, a title and a date');
      return;
    }
    setSaving(true);
    try {
      await hangOutService.create(user.id, {
        restaurantId: form.restaurantId,
        title: form.title,
        description: form.description,
        plannedDateTime: form.plannedDateTime,
        maxParticipants: form.maxParticipants,
        aiGenerated: !!aiResult,
      });
      toast.success('Hangout created!');
      navigate('/hangout');
    } catch {
      /* handled by apiClient */
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="create-hangout-page">
      <div className="page-header-create">
        <div className="container">
          <h1>Create a Hangout</h1>
          <p>Describe your idea — the AI assistant will plan it for you.</p>
        </div>
      </div>

      <div className="container create-hangout-layout">
        {/* AI Assistant */}
        <div className="ai-panel">
          <div className="ai-panel-header">
            <span className="ai-spark">✨</span>
            <div>
              <h2>AI Hangout Assistant</h2>
              <p>Tell us about your outing in plain words.</p>
            </div>
          </div>

          <div className="form-group">
            <label>Your idea</label>
            <textarea
              className="form-input"
              rows={4}
              value={prompt}
              placeholder="e.g. Birthday dinner for 6 friends, medium budget, we love Italian food, one vegetarian"
              onChange={(e) => setPrompt(e.target.value)}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Party size</label>
              <input
                type="number"
                className="form-input"
                min={2}
                max={50}
                value={partySize}
                onChange={(e) => setPartySize(parseInt(e.target.value) || 2)}
              />
            </div>
            <div className="form-group">
              <label>Budget (optional)</label>
              <select className="form-input" value={budget} onChange={(e) => setBudget(e.target.value)}>
                {BUDGETS.map((b) => (
                  <option key={b || 'any'} value={b}>
                    {b || 'Any'}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <button className="btn btn-primary btn-block" onClick={askAi} disabled={askingAi}>
            {askingAi ? 'Thinking…' : '✨ Ask the AI Assistant'}
          </button>

          {aiResult && (
            <div className="ai-result">
              {aiResult.reason && (
                <p className="ai-reason">
                  <strong>Why this pick:</strong> {aiResult.reason}
                </p>
              )}
              {aiResult.keywords?.length > 0 && (
                <div className="ai-keywords">
                  {aiResult.keywords.map((k) => (
                    <span key={k} className="ai-keyword">
                      {k}
                    </span>
                  ))}
                </div>
              )}
              {aiResult.notice && <p className="ai-notice">ℹ️ {aiResult.notice}</p>}
            </div>
          )}
        </div>

        {/* Hangout form */}
        <form className="hangout-form" onSubmit={handleSubmit}>
          <h2>Hangout details</h2>

          <div className="form-group">
            <label>Restaurant</label>
            <select
              className="form-input"
              value={form.restaurantId}
              onChange={(e) => setForm({ ...form, restaurantId: e.target.value })}
              required
            >
              <option value="">Select a restaurant…</option>
              {restaurants.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} — {r.cuisine}
                  {r.priceRange ? ` (${r.priceRange.toLowerCase()})` : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Title</label>
            <input
              className="form-input"
              value={form.title}
              placeholder="Give your hangout a catchy title"
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
            />
          </div>

          <div className="form-group">
            <label>Description / invitation</label>
            <textarea
              className="form-input"
              rows={3}
              value={form.description}
              placeholder="What's the plan?"
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Date &amp; time</label>
              <input
                type="datetime-local"
                className="form-input"
                value={form.plannedDateTime}
                min={new Date().toISOString().slice(0, 16)}
                onChange={(e) => setForm({ ...form, plannedDateTime: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Max participants</label>
              <input
                type="number"
                className="form-input"
                min={2}
                max={50}
                value={form.maxParticipants}
                onChange={(e) => setForm({ ...form, maxParticipants: parseInt(e.target.value) || 2 })}
              />
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Creating…' : 'Create Hangout'}
            </button>
            <button type="button" className="btn btn-outline" onClick={() => navigate('/hangout')}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateHangOutPage;
