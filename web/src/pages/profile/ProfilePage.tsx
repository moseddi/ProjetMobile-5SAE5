import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import './ProfilePage.css';

const ProfilePage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  if (!user) {
    return null;
  }

  return (
    <div className="profile-page">
      <div className="page-header">
        <div className="container">
          <h1>My Profile</h1>
          <p>Manage your account information</p>
        </div>
      </div>

      <div className="container">
        <div className="profile-layout">
          {/* Profile Card */}
          <div className="profile-card">
            <div className="profile-header">
              <img
                src={user.avatar || 'https://via.placeholder.com/120'}
                alt={user.name}
                className="profile-avatar"
              />
              <div className="profile-info">
                <h2>{user.name}</h2>
                <p className="profile-email">{user.email}</p>
                {user.phone && <p className="profile-phone">📞 {user.phone}</p>}
              </div>
            </div>

            <div className="profile-stats">
              <div className="stat-card">
                <div className="stat-icon">🎯</div>
                <div className="stat-content">
                  <span className="stat-value">{user.loyaltyPoints}</span>
                  <span className="stat-label">Loyalty Points</span>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">📅</div>
                <div className="stat-content">
                  <span className="stat-value">{user.totalReservations}</span>
                  <span className="stat-label">Reservations</span>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">🛍️</div>
                <div className="stat-content">
                  <span className="stat-value">{user.totalOrders}</span>
                  <span className="stat-label">Orders</span>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon">⭐</div>
                <div className="stat-content">
                  <span className="stat-value">{user.totalReviews}</span>
                  <span className="stat-label">Reviews</span>
                </div>
              </div>
            </div>

            <div className="profile-meta">
              <p>
                Member since {new Date(user.createdAt).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric'
                })}
              </p>
            </div>
          </div>

          {/* Actions */}
          <div className="profile-actions">
            <div className="action-card">
              <h3>Account Actions</h3>
              <div className="action-list">
                <button className="action-btn" onClick={() => navigate('/reservations')}>
                  <span className="action-icon">📅</span>
                  <div className="action-content">
                    <span className="action-title">My Reservations</span>
                    <span className="action-desc">View and manage your bookings</span>
                  </div>
                </button>

                <button className="action-btn" onClick={() => navigate('/reviews')}>
                  <span className="action-icon">⭐</span>
                  <div className="action-content">
                    <span className="action-title">My Reviews</span>
                    <span className="action-desc">See all your restaurant reviews</span>
                  </div>
                </button>

                <button className="action-btn" onClick={() => navigate('/restaurants')}>
                  <span className="action-icon">🍽️</span>
                  <div className="action-content">
                    <span className="action-title">Explore Restaurants</span>
                    <span className="action-desc">Discover new dining experiences</span>
                  </div>
                </button>

                <button className="action-btn danger" onClick={handleLogout}>
                  <span className="action-icon">🚪</span>
                  <div className="action-content">
                    <span className="action-title">Logout</span>
                    <span className="action-desc">Sign out of your account</span>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
