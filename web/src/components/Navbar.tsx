import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { isAuthenticated, logout, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-content">
          {/* Logo/Brand */}
          <Link to="/restaurants" className="navbar-brand">
            <span className="brand-text">HangOut</span>
          </Link>

          {/* Main Navigation - Public Pages */}
          <div className="navbar-nav">
            <Link 
              to="/restaurants" 
              className={`nav-link ${isActive('/restaurants') ? 'active' : ''}`}
            >
              Restaurants
            </Link>

            <Link 
              to="/reviews" 
              className={`nav-link ${isActive('/reviews') ? 'active' : ''}`}
            >
              Reviews
            </Link>

            <Link 
              to="/events" 
              className={`nav-link ${isActive('/events') ? 'active' : ''}`}
            >
              Events
            </Link>

            <Link 
              to="/hangout" 
              className={`nav-link ${isActive('/hangout') ? 'active' : ''}`}
            >
              Hang Out
            </Link>

            {/* Show Reservations and Loyalty when logged in */}
            {isAuthenticated && (
              <>
                <Link 
                  to="/reservations" 
                  className={`nav-link ${isActive('/reservations') ? 'active' : ''}`}
                >
                  Reservations
                </Link>

                <Link 
                  to="/loyalty" 
                  className={`nav-link ${isActive('/loyalty') ? 'active' : ''}`}
                >
                  Loyalty
                </Link>
              </>
            )}
          </div>

          {/* Right Actions */}
          <div className="navbar-actions">
            {isAuthenticated ? (
              <>
                <Link to="/profile" className="nav-link-icon" title="Profile">
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                    <path d="M10 10C12.7614 10 15 7.76142 15 5C15 2.23858 12.7614 0 10 0C7.23858 0 5 2.23858 5 5C5 7.76142 7.23858 10 10 10Z" fill="currentColor"/>
                    <path d="M10 12C4.477 12 0 14.477 0 17.5V20H20V17.5C20 14.477 15.523 12 10 12Z" fill="currentColor"/>
                  </svg>
                  <span className="user-name-desktop">{user?.name?.split(' ')[0]}</span>
                </Link>
                <button onClick={handleLogout} className="btn-text">
                  Sign Out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-text">
                  Sign In
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm">
                  Get Started
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
