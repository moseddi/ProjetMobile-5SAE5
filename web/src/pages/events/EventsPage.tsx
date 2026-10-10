import './EventsPage.css';
import { useCallback, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import EventAIForm from '../../components/EventAIForm';
import { eventService } from '../../services/event/eventService';
import type { Event as AppEvent } from '../../types';

const DEFAULT_IMAGE =
  'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=80';

const EventsPage = () => {
  const [showAIForm, setShowAIForm] = useState(false);
  const [events, setEvents] = useState<AppEvent[]>([]);
  const [restaurantNames, setRestaurantNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadEvents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [eventList, restaurants] = await Promise.all([
        eventService.getAll(),
        eventService.getRestaurants().catch(() => []),
      ]);

      const names: Record<string, string> = {};
      restaurants.forEach((r) => {
        names[r.id] = r.name;
      });
      setRestaurantNames(names);

      // Tri par date croissante
      const sorted = [...eventList].sort(
        (a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime()
      );
      setEvents(sorted);
    } catch {
  setError('Impossible de charger les événements. Vérifiez que le backend est démarré.');
} finally {
  setLoading(false);
}
  }, []);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="events-page">
      {/* Hero Section */}
      <section className="events-hero">
        <div className="container-wide">
          <div className="hero-content-events">
            <h1 className="hero-title-events">
              Exclusive <span className="accent-text">Culinary Events</span>
            </h1>
            <p className="hero-subtitle-events">
              Join unforgettable dining experiences and special occasions
            </p>
          </div>
        </div>
      </section>

      {/* AI form */}
      <section className="events-section">
        <div className="container-wide">
          <div className="events-toolbar">
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setShowAIForm(!showAIForm)}
            >
              {showAIForm ? 'Close form' : '✨ New event with AI'}
            </button>
          </div>

          {showAIForm && (
            <EventAIForm
              onSaved={() => {
                setShowAIForm(false);
                loadEvents();
              }}
            />
          )}
        </div>
      </section>

      {/* Events Grid */}
      <section className="events-section">
        <div className="container-wide">
          {loading && <p className="events-message">Loading events…</p>}

          {!loading && error && <p className="events-message events-message--error">{error}</p>}

          {!loading && !error && events.length === 0 && (
            <p className="events-message">No events yet. Create the first one with the AI button above.</p>
          )}

          <div className="events-grid">
            {events.map((event) => {
              const spotsLeft = (event.availableSpots ?? 0) - (event.bookedSpots ?? 0);
              const soldOut = spotsLeft <= 0;

              return (
                <div key={event.id} className="event-card">
                  <div className="event-image-wrapper">
                    <img
                      src={event.image || DEFAULT_IMAGE}
                      alt={event.title}
                      className="event-image"
                      onError={(e) => {
                        e.currentTarget.src = DEFAULT_IMAGE;
                      }}
                    />
                    <div className="event-date-badge">
                      <span className="event-month">
                        {new Date(event.eventDate).toLocaleDateString('en-US', { month: 'short' })}
                      </span>
                      <span className="event-day">
                        {new Date(event.eventDate).toLocaleDateString('en-US', { day: 'numeric' })}
                      </span>
                    </div>
                    {event.generatedByAI && <span className="event-ai-badge">✨ AI</span>}
                  </div>

                  <div className="event-content">
                    <h3 className="event-title">{event.title}</h3>
                    <p className="event-restaurant">
                      {restaurantNames[event.restaurantId] ?? 'Restaurant'}
                    </p>
                    <p className="event-description">{event.description}</p>

                    {event.hashtags && event.hashtags.length > 0 && (
                      <p className="event-hashtags">{event.hashtags.join(' ')}</p>
                    )}

                    {event.appelAction && (
                      <p className="event-cta-text">{event.appelAction}</p>
                    )}

                    <div className="event-footer-info">
                      <div className="event-time">
                        <span className="event-icon">🕐</span>
                        <span>{formatTime(event.eventDate)}</span>
                      </div>
                      <div className="event-price">${event.price} per person</div>
                    </div>

                    <p className="event-spots">
                      {soldOut ? 'Sold out' : `${spotsLeft} spots left`}
                    </p>

                    <button
                      className="btn btn-primary btn-sm btn-full-mobile"
                      disabled={soldOut}
                    >
                      {soldOut ? 'Sold out' : 'Reserve Spot'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
};

export default EventsPage;