import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { reservationService } from '../../services/reservation/reservationService';
import { Reservation } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './ReservationsPage.css';

const ReservationsPage = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'upcoming' | 'past'>('all');

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
    }
  }, [user]);

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
      await reservationService.create(user.id, {
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

  return (
    <div className="reservations-page">
      <div className="page-header">
        <div className="container">
          <h1>My Reservations</h1>
          <p>Manage your restaurant reservations</p>
        </div>
      </div>

      <div className="container">
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
                  <h3>Restaurant #{reservation.restaurantId}</h3>
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
      </div>
    </div>
  );
};

export default ReservationsPage;
