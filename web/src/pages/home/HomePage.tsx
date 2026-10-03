import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { restaurantService } from '../../services/restaurant/restaurantService';
import { reservationService } from '../../services/reservation/reservationService';
import { Restaurant, Reservation } from '../../types';
import { useAuth } from '../../context/AuthContext';
import './HomePage.css';

const HomePage = () => {
  const { user } = useAuth();
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [featuredRestaurant, setFeaturedRestaurant] = useState<Restaurant | null>(null);
  const [upcomingReservation, setUpcomingReservation] = useState<Reservation | null>(null);
  const [loading, setLoading] = useState(true);

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const loadData = async () => {
    try {
      const allRestaurants = await restaurantService.getAll();
      setRestaurants(allRestaurants);
      
      // Set highest rated as featured
      const featured = allRestaurants.reduce((prev, current) => 
        (current.rating > prev.rating) ? current : prev
      );
      setFeaturedRestaurant(featured);

      // Load upcoming reservation if user is logged in
      if (user?.id) {
        try {
          const reservations = await reservationService.getUserReservations(user.id);
          const upcoming = reservations.find(r => 
            new Date(r.reservationDateTime) > new Date() && 
            (r.status === 'CONFIRMED' || r.status === 'PENDING')
          );
          setUpcomingReservation(upcoming || null);
        } catch (error) {
          console.error('Failed to load reservations:', error);
        }
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getTimeBasedSuggestion = () => {
    const hour = new Date().getHours();
    if (hour < 11) return 'Perfect spots for breakfast';
    if (hour < 15) return 'Great lunch destinations';
    if (hour < 18) return 'Afternoon delights';
    return 'Dinner reservations filling fast';
  };

  if (loading) {
    return (
      <div className="home-page">
        <div className="loading-container">
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="home-page">
      {/* Hero Welcome Section */}
      <section className="hero-welcome">
        <div className="container">
          <div className="welcome-content">
            <h1 className="greeting">
              {getGreeting()}{user ? `, ${user.name.split(' ')[0]}` : ''}.
            </h1>
            <p className="tagline">
              Discover something worth experiencing tonight
            </p>
            
            <div className="search-box">
              <input 
                type="text" 
                placeholder="Search restaurants, cuisines, or neighborhoods..."
                className="search-input"
              />
              <button className="search-btn">Search</button>
            </div>
          </div>
        </div>
      </section>

      <div className="container">
        {/* Main Content Grid */}
        <div className="content-grid">
          {/* Featured Restaurant - Hero */}
          {featuredRestaurant && (
            <Link to={`/restaurants/${featuredRestaurant.id}`} className="featured-restaurant">
              <div className="featured-image">
                <img src={featuredRestaurant.image} alt={featuredRestaurant.name} />
                <div className="featured-overlay">
                  <span className="featured-label">Chef's Selection</span>
                  <h2 className="featured-name">{featuredRestaurant.name}</h2>
                  <div className="featured-meta">
                    <span>{featuredRestaurant.cuisine}</span>
                    <span className="separator">·</span>
                    <span className="rating-display">
                      {featuredRestaurant.rating.toFixed(1)} ★
                    </span>
                  </div>
                </div>
              </div>
            </Link>
          )}

          {/* Upcoming Reservation Card */}
          {upcomingReservation && (
            <div className="reservation-card">
              <div className="card-label">Your Reservation</div>
              <div className="reservation-details">
                <h3>Restaurant #{upcomingReservation.restaurantId}</h3>
                <div className="reservation-date">
                  {new Date(upcomingReservation.reservationDateTime).toLocaleDateString('en-US', {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric'
                  })}
                </div>
                <div className="reservation-time">
                  {new Date(upcomingReservation.reservationDateTime).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </div>
                <div className="reservation-party">
                  {upcomingReservation.partySize} {upcomingReservation.partySize === 1 ? 'guest' : 'guests'}
                </div>
              </div>
              <Link to="/reservations" className="reservation-link">View details →</Link>
            </div>
          )}
        </div>

        {/* Curated Restaurants */}
        <section className="curated-section">
          <div className="section-intro">
            <h2 className="section-heading">{getTimeBasedSuggestion()}</h2>
            <Link to="/restaurants" className="section-action">Explore all</Link>
          </div>

          <div className="restaurant-showcase">
            {restaurants.slice(0, 6).map((restaurant) => (
              <Link 
                key={restaurant.id} 
                to={`/restaurants/${restaurant.id}`}
                className="restaurant-item"
              >
                <div className="restaurant-image-wrapper">
                  <img src={restaurant.image} alt={restaurant.name} />
                  {!restaurant.isOpen && (
                    <div className="status-badge closed">Closed</div>
                  )}
                </div>
                <div className="restaurant-info">
                  <h3 className="restaurant-title">{restaurant.name}</h3>
                  <div className="restaurant-details">
                    <span className="cuisine-text">{restaurant.cuisine}</span>
                    <span className="detail-separator">·</span>
                    <span className="rating-text">{restaurant.rating.toFixed(1)} ★</span>
                  </div>
                  <p className="restaurant-snippet">{restaurant.description}</p>
                </div>
              </Link>
            ))}
          </div>
        </section>

        {/* Experience Types */}
        <section className="experience-types">
          <h2 className="section-heading">Plan your next experience</h2>
          
          <div className="experience-grid">
            <Link to="/restaurants" className="experience-card dining">
              <div className="experience-content">
                <h3>Dining</h3>
                <p>Discover exceptional restaurants</p>
              </div>
            </Link>
            
            <Link to="/reviews" className="experience-card reviews">
              <div className="experience-content">
                <h3>Reviews</h3>
                <p>Authentic dining experiences</p>
              </div>
            </Link>
            
            <Link to="/reservations" className="experience-card reservations">
              <div className="experience-content">
                <h3>Reservations</h3>
                <p>Secure your table</p>
              </div>
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
};

export default HomePage;
