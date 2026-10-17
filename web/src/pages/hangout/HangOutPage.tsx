import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { hangOutService } from '../../services/hangout/hangoutService';
import { HangOut } from '../../types';
import './HangOutPage.css';

type Filter = 'all' | 'upcoming' | 'mine' | 'joined';

const HangOutPage = () => {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [hangouts, setHangouts] = useState<HangOut[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>('all');

  useEffect(() => {
    loadHangouts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, user?.id]);

  const loadHangouts = async () => {
    setLoading(true);
    try {
      let data: HangOut[];
      if (filter === 'mine' && user?.id) {
        data = await hangOutService.getByCreator(user.id);
      } else if (filter === 'joined' && user?.id) {
        data = await hangOutService.getJoined(user.id);
      } else if (filter === 'upcoming') {
        data = await hangOutService.getAll(true);
      } else {
        data = await hangOutService.getAll();
      }
      setHangouts(data);
    } catch (error) {
      console.error('Failed to load hangouts:', error);
    } finally {
      setLoading(false);
    }
  };

  const requireAuth = (path: string, message: string) => {
    if (!isAuthenticated) {
      toast.info(message);
      navigate('/login', { state: { from: path } });
      return false;
    }
    return true;
  };

  const handleCreate = () => {
    if (requireAuth('/hangout/create', 'Please sign in to create a hangout')) {
      navigate('/hangout/create');
    }
  };

  const handleFilters = (next: Filter) => {
    if ((next === 'mine' || next === 'joined') && !isAuthenticated) {
      toast.info('Please sign in to see your hangouts');
      navigate('/login', { state: { from: '/hangout' } });
      return;
    }
    setFilter(next);
  };

  const handleJoin = async (hangout: HangOut) => {
    if (!requireAuth('/hangout', 'Please sign in to join a hangout')) return;
    if (!user?.id) return;
    try {
      await hangOutService.join(hangout.id, user.id);
      toast.success(`You joined "${hangout.title}"!`);
      loadHangouts();
    } catch {
      /* handled by apiClient */
    }
  };

  const formatDate = (value: string) =>
    new Date(value).toLocaleString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  const isParticipant = (hangout: HangOut) =>
    !!user && hangout.participants?.some((p) => p.userId === user.id);

  const isCreator = (hangout: HangOut) => !!user && hangout.creatorId === user.id;

  return (
    <div className="hangout-page">
      <section className="hangout-hero">
        <div className="container-wide">
          <div className="hero-content-hangout">
            <span className="hero-kicker">Social Dining</span>
            <h1 className="hero-title-hangout">
              Hang Out &amp; <span className="accent-text">Dine Together</span>
            </h1>
            <p className="hero-subtitle-hangout">
              Organize a meal, invite friends and meet new people. Let our AI assistant plan the perfect hangout for you.
            </p>
            <div className="hero-actions-hangout">
              <button className="btn btn-primary btn-lg" onClick={handleCreate}>
                + Create a Hangout
              </button>
              <button
                className="btn btn-outline btn-lg"
                onClick={() =>
                  requireAuth('/hangout/create', 'Please sign in to use the AI assistant') &&
                  navigate('/hangout/create')
                }
              >
                ✨ Plan with AI
              </button>
            </div>
          </div>
        </div>
      </section>

      <section className="hangouts-section">
        <div className="container-wide">
          <div className="hangout-toolbar">
            <div className="section-header-hangout">
              <h2 className="section-title-hangout">Hangouts</h2>
              <p className="section-subtitle-hangout">Browse, join or manage your dining experiences</p>
            </div>
            <div className="filter-tabs-hangout">
              {(['all', 'upcoming', 'mine', 'joined'] as Filter[]).map((f) => (
                <button
                  key={f}
                  className={`filter-tab-hangout ${filter === f ? 'active' : ''}`}
                  onClick={() => handleFilters(f)}
                >
                  {f === 'all' ? 'All' : f === 'upcoming' ? 'Upcoming' : f === 'mine' ? 'Organized by me' : 'Joined'}
                </button>
              ))}
            </div>
          </div>

          {loading ? (
            <div className="loading">
              <div className="spinner"></div>
            </div>
          ) : hangouts.length === 0 ? (
            <div className="empty-state">
              <h3>No hangouts yet</h3>
              <p>Be the first to organize a dining experience.</p>
              <button className="btn btn-primary" onClick={handleCreate}>
                Create a Hangout
              </button>
            </div>
          ) : (
            <div className="hangouts-grid">
              {hangouts.map((hangout) => (
                <div
                  key={hangout.id}
                  className="hangout-card"
                  onClick={() => navigate(`/hangout/${hangout.id}`)}
                >
                  <div className="hangout-card-media">
                    {hangout.restaurantImage ? (
                      <img src={hangout.restaurantImage} alt={hangout.restaurantName} />
                    ) : (
                      <div className="hangout-card-media-fallback">{hangout.restaurantName?.[0] || 'H'}</div>
                    )}
                    <span className={`status-badge-hangout status-${hangout.status.toLowerCase()}`}>
                      {hangout.status}
                    </span>
                    {hangout.aiGenerated && <span className="ai-badge-hangout">✨ AI</span>}
                  </div>

                  <div className="hangout-card-body">
                    <div className="hangout-card-header">
                      <h3 className="hangout-title">{hangout.title}</h3>
                      <div className="hangout-restaurant">{hangout.restaurantName}</div>
                    </div>

                    <div className="hangout-details">
                      <div className="hangout-detail-item">
                        <span className="detail-icon">📅</span>
                        <span>{formatDate(hangout.plannedDateTime)}</span>
                      </div>
                      <div className="hangout-detail-item">
                        <span className="detail-icon">👥</span>
                        <span>
                          {hangout.joinedCount} / {hangout.maxParticipants} joined
                        </span>
                      </div>
                      {hangout.restaurantCuisine && (
                        <div className="hangout-detail-item">
                          <span className="detail-icon">🍽️</span>
                          <span>{hangout.restaurantCuisine}</span>
                        </div>
                      )}
                    </div>

                    {hangout.description && (
                      <p className="hangout-description">{hangout.description}</p>
                    )}
                  </div>

                  <div className="hangout-footer">
                    <div className="hangout-creator">
                      <span className="creator-label">Organized by</span>
                      <span className="creator-name">{hangout.creatorName}</span>
                    </div>

                    {isCreator(hangout) ? (
                      <button className="btn btn-outline btn-sm" onClick={(e) => { e.stopPropagation(); navigate(`/hangout/${hangout.id}`); }}>
                        Manage
                      </button>
                    ) : isParticipant(hangout) ? (
                      <span className="joined-pill">✓ Joined</span>
                    ) : hangout.status === 'OPEN' ? (
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleJoin(hangout);
                        }}
                      >
                        Join
                      </button>
                    ) : (
                      <span className="joining-closed">{hangout.status === 'FULL' ? 'Full' : hangout.status}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default HangOutPage;
