import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { reservationService } from '../../services/reservation/reservationService';
import { restaurantService } from '../../services/restaurant/restaurantService';
import { eventService } from '../../services/event/eventService';
import { eventBookingService } from '../../services/eventBooking/eventBookingService';
import { EventBooking, Reservation } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import ReservationChatbot, { ChatbotMode } from '../../components/ReservationChatbot';
import './ReservationsPage.css';

const ReservationsPage = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [eventBookings, setEventBookings] = useState<EventBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'past'>('all');
  const [restaurantNames, setRestaurantNames] = useState<Record<string, string>>({});
  const [eventTitles, setEventTitles] = useState<Record<string, string>>({});
  const [section, setSection] = useState<'restaurants' | 'events'>('restaurants');
  const [chatbot, setChatbot] = useState<ChatbotMode | null>(null);

  // New reservation form (if coming from restaurant detail)
  const restaurantId = location.state?.restaurantId;
  const [showForm, setShowForm] = useState(!!restaurantId);
  const [formData, setFormData] = useState({
    reservationDateTime: '',
    partySize: 2,
    specialRequests: ''
  });

  useEffect(() => {
    if (user) {
      loadReservations();
      loadEventBookings();
    }
  }, [user]);

  // Show restaurant / event names instead of raw ids
  useEffect(() => {
    restaurantService.getAll()
      .then((list) => setRestaurantNames(Object.fromEntries(list.map((r) => [r.id, r.name]))))
      .catch(() => setRestaurantNames({}));
    eventService.getAll()
      .then((list) => setEventTitles(Object.fromEntries(list.map((e) => [e.id, e.title]))))
      .catch(() => setEventTitles({}));
  }, []);

  const loadReservations = async () => {
    try {
      if (!user?.id) return;
      const data = await reservationService.getUserReservations(user.id);
      setReservations(data);
    } catch (error) {
      console.error('Failed to load reservations:', error);
      toast.error('Failed to load reservations');
    } finally {
      setLoading(false);
    }
  };

  const loadEventBookings = async () => {
    try {
      if (!user?.id) return;
      const data = await eventBookingService.getUserBookings(user.id);
      setEventBookings(data);
    } catch (error) {
      console.error('Failed to load event bookings:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!restaurantId) {
      toast.error('Restaurant ID is missing');
      return;
    }

    try {
      if (!user?.id) {
        toast.error('User not logged in');
        return;
      }
      await reservationService.create({
        restaurantId,
        ...formData
      });
      toast.success('Reservation created successfully!');
      setShowForm(false);
      loadReservations();
    } catch (error) {
      console.error('Failed to create reservation:', error);
      toast.error('Failed to create reservation');
    }
  };

  const handleCancel = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this reservation?')) {
      return;
    }

    try {
      await reservationService.cancel(id);
      toast.success('Reservation cancelled');
      loadReservations();
    } catch (error) {
      console.error('Failed to cancel reservation:', error);
      toast.error('Failed to cancel reservation');
    }
  };

  const handleCancelEventBooking = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) {
      return;
    }

    try {
      await eventBookingService.cancel(id);
      toast.success('Booking cancelled');
      loadEventBookings();
    } catch (error) {
      console.error('Failed to cancel event booking:', error);
      toast.error('Failed to cancel booking');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED': return 'status-confirmed';
      case 'PENDING': return 'status-pending';
      case 'CANCELLED': return 'status-cancelled';
      case 'COMPLETED': return 'status-completed';
      default: return '';
    }
  };

  const filteredReservations = reservations.filter(r => {
    if (filter === 'all') return true;
    const isPast = new Date(r.reservationDateTime) < new Date();
    if (filter === 'upcoming') return !isPast;
    if (filter === 'past') return isPast;
    return true;
  });

  const filteredEventBookings = eventBookings;

  return (
    <div className="reservations-page">
      <div className="page-header">
        <div className="container">
          <h1>My Reservations</h1>
          <p>Manage your restaurant and event reservations</p>
        </div>
      </div>

      <div className="container">
        {/* Call to action: open the assistant for a restaurant table or an event */}
        <div className="reservation-cta-row">
          <button type="button" className="btn btn-primary" onClick={() => setChatbot('restaurant')}>
            🍽️ Reserve a Restaurant Table
          </button>
          <button type="button" className="btn btn-outline" onClick={() => setChatbot('event')}>
            🎉 Reserve an Event
          </button>
        </div>

        {/* New Reservation Form */}
        {showForm && (
          <div className="reservation-form-card">
            <h2>New Reservation</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label>Date & Time</label>
                  <input
                    type="datetime-local"
                    className="form-input"
                    value={formData.reservationDateTime}
                    onChange={(e) => setFormData({ ...formData, reservationDateTime: e.target.value })}
                    required
                    min={new Date().toISOString().slice(0, 16)}
                  />
                </div>

                <div className="form-group">
                  <label>Party Size</label>
                  <input
                    type="number"
                    className="form-input"
                    value={formData.partySize}
                    onChange={(e) => setFormData({ ...formData, partySize: parseInt(e.target.value) })}
                    required
                    min={1}
                    max={20}
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Special Requests (optional)</label>
                <textarea
                  className="form-input"
                  value={formData.specialRequests}
                  onChange={(e) => setFormData({ ...formData, specialRequests: e.target.value })}
                  rows={3}
                  placeholder="e.g., Window seat, high chair needed, dietary restrictions..."
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="btn btn-primary">
                  Confirm Reservation
                </button>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Restaurant vs Event section toggle */}
        <div className="filter-tabs">
          <button
            className={`filter-tab ${section === 'restaurants' ? 'active' : ''}`}
            onClick={() => setSection('restaurants')}
          >
            Restaurant Tables
          </button>
          <button
            className={`filter-tab ${section === 'events' ? 'active' : ''}`}
            onClick={() => setSection('events')}
          >
            Events
          </button>
        </div>

        {section === 'restaurants' && (
          <>
            {/* Filters */}
            <div className="filter-tabs">
              <button
                className={`filter-tab ${filter === 'all' ? 'active' : ''}`}
                onClick={() => setFilter('all')}
              >
                All Reservations
              </button>
              <button
                className={`filter-tab ${filter === 'upcoming' ? 'active' : ''}`}
                onClick={() => setFilter('upcoming')}
              >
                Upcoming
              </button>
              <button
                className={`filter-tab ${filter === 'past' ? 'active' : ''}`}
                onClick={() => setFilter('past')}
              >
                Past
              </button>
            </div>

            {/* Reservations List */}
            {loading ? (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Loading reservations...</p>
              </div>
            ) : filteredReservations.length === 0 ? (
              <div className="empty-state">
                <p>No reservations found</p>
              </div>
            ) : (
              <div className="reservations-list">
                {filteredReservations.map(reservation => (
                  <div key={reservation.id} className="reservation-card">
                    <div className="reservation-header">
                      <h3>{restaurantNames[reservation.restaurantId] ?? `Restaurant #${reservation.restaurantId}`}</h3>
                      <span className={`status-badge ${getStatusColor(reservation.status)}`}>
                        {reservation.status}
                      </span>
                    </div>

                    <div className="reservation-details">
                      <div className="detail-item">
                        <span className="detail-label">📅 Date & Time</span>
                        <span className="detail-value">
                          {new Date(reservation.reservationDateTime).toLocaleString('en-US', {
                            weekday: 'long',
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </span>
                      </div>

                      <div className="detail-item">
                        <span className="detail-label">👥 Party Size</span>
                        <span className="detail-value">{reservation.partySize} guests</span>
                      </div>

                      {reservation.specialRequests && (
                        <div className="detail-item">
                          <span className="detail-label">📝 Special Requests</span>
                          <span className="detail-value">{reservation.specialRequests}</span>
                        </div>
                      )}

                      <div className="detail-item">
                        <span className="detail-label">Created</span>
                        <span className="detail-value">
                          {new Date(reservation.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>

                    {reservation.status !== 'CANCELLED' && reservation.status !== 'COMPLETED' && (
                      <div className="reservation-actions">
                        <button
                          className="btn btn-danger-outline"
                          onClick={() => handleCancel(reservation.id)}
                        >
                          Cancel Reservation
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {section === 'events' && (
          <>
            {loading ? (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Loading bookings...</p>
              </div>
            ) : filteredEventBookings.length === 0 ? (
              <div className="empty-state">
                <p>No event bookings found</p>
              </div>
            ) : (
              <div className="reservations-list">
                {filteredEventBookings.map(booking => (
                  <div key={booking.id} className="reservation-card">
                    <div className="reservation-header">
                      <h3>{eventTitles[booking.eventId] ?? `Event #${booking.eventId}`}</h3>
                      <span className={`status-badge ${getStatusColor(booking.status)}`}>
                        {booking.status}
                      </span>
                    </div>

                    <div className="reservation-details">
                      <div className="detail-item">
                        <span className="detail-label">👥 Party Size</span>
                        <span className="detail-value">{booking.partySize} guests</span>
                      </div>

                      <div className="detail-item">
                        <span className="detail-label">Created</span>
                        <span className="detail-value">
                          {new Date(booking.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>

                    {booking.status !== 'CANCELLED' && (
                      <div className="reservation-actions">
                        <button
                          className="btn btn-danger-outline"
                          onClick={() => handleCancelEventBooking(booking.id)}
                        >
                          Cancel Booking
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>

      {/* AI reservation assistant: hidden until the user clicks one of the two buttons above */}
      {chatbot && (
        <ReservationChatbot
          mode={chatbot}
          restaurantId={chatbot === 'restaurant' ? restaurantId : undefined}
          onReservationChanged={() => {
            loadReservations();
            loadEventBookings();
          }}
          onClose={() => setChatbot(null)}
        />
      )}
    </div>
  );
};

export default ReservationsPage;
