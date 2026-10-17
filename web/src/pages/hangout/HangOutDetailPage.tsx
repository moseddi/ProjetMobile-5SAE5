import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { hangOutService } from '../../services/hangout/hangoutService';
import { HangOut } from '../../types';
import './HangOutDetailPage.css';

const HangOutDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [hangout, setHangout] = useState<HangOut | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);

  useEffect(() => {
    if (id) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const load = async () => {
    try {
      setLoading(true);
      const data = await hangOutService.getById(id!);
      setHangout(data);
    } catch {
      /* handled by apiClient */
    } finally {
      setLoading(false);
    }
  };

  const requireAuth = (message: string) => {
    if (!isAuthenticated) {
      toast.info(message);
      navigate('/login', { state: { from: `/hangout/${id}` } });
      return false;
    }
    return true;
  };

  const doAction = async (action: 'join' | 'leave' | 'cancel') => {
    if (!requireAuth('Please sign in first')) return;
    if (!user?.id || !hangout) return;
    setWorking(true);
    try {
      if (action === 'join') {
        await hangOutService.join(hangout.id, user.id);
        toast.success('You joined the hangout!');
      } else if (action === 'leave') {
        await hangOutService.leave(hangout.id, user.id);
        toast.info('You left the hangout');
      } else {
        if (!window.confirm('Cancel this hangout?')) return;
        await hangOutService.cancel(hangout.id, user.id);
        toast.info('Hangout cancelled');
      }
      await load();
    } catch {
      /* handled by apiClient */
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
      </div>
    );
  }

  if (!hangout) {
    return (
      <div className="container empty-state">
        <h3>Hangout not found</h3>
        <button className="btn btn-outline" onClick={() => navigate('/hangout')}>
          Back to hangouts
        </button>
      </div>
    );
  }

  const isCreator = !!user && hangout.creatorId === user.id;
  const isParticipant = !!user && hangout.participants?.some((p) => p.userId === user.id);
  const spotsLeft = Math.max(0, hangout.maxParticipants - hangout.joinedCount);
  const canJoin = hangout.status === 'OPEN' && !isParticipant;

  return (
    <div className="hangout-detail-page">
      <div className="hangout-detail-hero">
        {hangout.restaurantImage && (
          <img className="hangout-detail-hero-img" src={hangout.restaurantImage} alt={hangout.restaurantName} />
        )}
        <div className="hangout-detail-hero-overlay">
          <div className="container">
            <button className="back-link" onClick={() => navigate('/hangout')}>
              ← All hangouts
            </button>
            <div className="detail-hero-badges">
              <span className={`status-badge-hangout status-${hangout.status.toLowerCase()}`}>{hangout.status}</span>
              {hangout.aiGenerated && <span className="ai-badge-hangout">✨ AI planned</span>}
            </div>
            <h1 className="hangout-detail-title">{hangout.title}</h1>
            <p className="hangout-detail-restaurant">
              at {hangout.restaurantName}
              {hangout.restaurantCuisine ? ` · ${hangout.restaurantCuisine}` : ''}
            </p>
          </div>
        </div>
      </div>

      <div className="container hangout-detail-layout">
        <div className="hangout-detail-main">
          <div className="detail-card">
            <h3>About this hangout</h3>
            <p className="hangout-detail-description">{hangout.description || 'No description provided.'}</p>

            <div className="detail-info-grid">
              <div className="detail-info-item">
                <span className="detail-info-label">📅 When</span>
                <span className="detail-info-value">
                  {new Date(hangout.plannedDateTime).toLocaleString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>
              <div className="detail-info-item">
                <span className="detail-info-label">👥 Capacity</span>
                <span className="detail-info-value">
                  {hangout.joinedCount} / {hangout.maxParticipants} · {spotsLeft} spot{spotsLeft === 1 ? '' : 's'} left
                </span>
              </div>
              <div className="detail-info-item">
                <span className="detail-info-label">🍽️ Cuisine</span>
                <span className="detail-info-value">{hangout.restaurantCuisine || '—'}</span>
              </div>
              <div className="detail-info-item">
                <span className="detail-info-label">💰 Price range</span>
                <span className="detail-info-value">{hangout.restaurantPriceRange || '—'}</span>
              </div>
            </div>
          </div>

          <div className="detail-card">
            <h3>Participants ({hangout.joinedCount})</h3>
            <div className="participants-list">
              {hangout.participants?.map((p) => (
                <div key={p.userId} className="participant">
                  {p.avatar ? (
                    <img src={p.avatar} alt={p.name} />
                  ) : (
                    <div className="participant-avatar-fallback">{p.name?.[0] || '?'}</div>
                  )}
                  <div className="participant-info">
                    <span className="participant-name">
                      {p.name}
                      {p.userId === hangout.creatorId && <span className="organizer-tag">Organizer</span>}
                    </span>
                    <span className="participant-status">{p.status.toLowerCase()}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <aside className="hangout-detail-sidebar">
          <div className="detail-card sticky-card">
            <span className="creator-label">Organized by</span>
            <div className="sidebar-creator">
              {hangout.creatorAvatar ? (
                <img src={hangout.creatorAvatar} alt={hangout.creatorName} />
              ) : (
                <div className="participant-avatar-fallback">{hangout.creatorName?.[0] || '?'}</div>
              )}
              <span>{hangout.creatorName}</span>
            </div>

            {isCreator ? (
              <>
                <p className="sidebar-hint">You are the organizer.</p>
                {hangout.status !== 'CANCELLED' && (
                  <button className="btn btn-danger-outline btn-block" disabled={working} onClick={() => doAction('cancel')}>
                    Cancel hangout
                  </button>
                )}
              </>
            ) : isParticipant ? (
              <>
                <p className="sidebar-hint sidebar-hint-success">✓ You are going!</p>
                <button className="btn btn-outline btn-block" disabled={working} onClick={() => doAction('leave')}>
                  Leave hangout
                </button>
              </>
            ) : canJoin ? (
              <button className="btn btn-primary btn-block" disabled={working} onClick={() => doAction('join')}>
                Join this hangout
              </button>
            ) : (
              <p className="sidebar-hint">
                {hangout.status === 'FULL' ? 'This hangout is full.' : `This hangout is ${hangout.status.toLowerCase()}.`}
              </p>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
};

export default HangOutDetailPage;
