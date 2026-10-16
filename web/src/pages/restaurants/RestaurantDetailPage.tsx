import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import RestaurantFormModal from '../../components/RestaurantFormModal';
import { resolveRestaurantMediaUrl, restaurantService } from '../../services/restaurant/restaurantService';
import { reviewService } from '../../services/review/reviewService';
import { Restaurant, Review } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './RestaurantDetailPage.css';

const RestaurantDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);

  useEffect(() => {
    if (id) {
      loadRestaurantDetails(id);
      loadReviews(id);
    }
  }, [id]);

  const loadRestaurantDetails = async (restaurantId: string) => {
    try {
      const data = await restaurantService.getById(restaurantId);
      setRestaurant(data);
    } catch (error) {
      console.error('Failed to load restaurant:', error);
      toast.error('Failed to load restaurant details');
    } finally {
      setLoading(false);
    }
  };

  const loadReviews = async (restaurantId: string) => {
    try {
      const data = await reviewService.getByRestaurant(restaurantId);
      setReviews(data);
    } catch (error) {
      console.error('Failed to load reviews:', error);
    }
  };

  const handleDelete = async () => {
    if (!restaurant || !window.confirm(`Delete "${restaurant.name}"?`)) {
      return;
    }

    try {
      await restaurantService.remove(restaurant.id);
      toast.success('Restaurant deleted');
      navigate('/restaurants');
    } catch (error) {
      console.error('Failed to delete restaurant:', error);
    }
  };

  const handleReservation = () => {
    if (!user) {
      toast.info('Please sign in to make a reservation');
      navigate('/login', { state: { from: `/restaurants/${id}`, action: 'reserve' } });
      return;
    }
    navigate('/reservations', { state: { restaurantId: id } });
  };

  const handleWriteReview = () => {
    if (!user) {
      toast.info('Please sign in to write a review');
      navigate('/login', { state: { from: `/restaurants/${id}`, action: 'review' } });
      return;
    }
    navigate('/reviews', { state: { restaurantId: id } });
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  if (!restaurant) {
    return (
      <div className="error-container">
        <h2>Restaurant not found</h2>
        <button className="btn btn-primary" onClick={() => navigate('/restaurants')}>
          Back to Restaurants
        </button>
      </div>
    );
  }

  return (
    <div className="restaurant-detail-page">
      {/* Hero Image Section */}
      <div className="restaurant-hero">
        <img src={restaurant.image} alt={restaurant.name} className="hero-image" />
      </div>

      {/* Restaurant Header */}
      <div className="restaurant-header">
        <div className="container-wide">
          <div className="header-content">
            <div className="header-main">
              <h1 className="restaurant-title">{restaurant.name}</h1>
              <div className="restaurant-meta">
                <span className="meta-item cuisine">{restaurant.cuisine}</span>
                <span className="meta-separator">·</span>
                <span className="meta-item rating">
                  {(restaurant.rating ?? 0).toFixed(1)} ★
                </span>
                <span className="meta-separator">·</span>
                <span className="meta-item">{restaurant.reviewCount} reviews</span>
                <span className="meta-separator">·</span>
                <span className={`meta-item status ${restaurant.isOpen ? 'open' : 'closed'}`}>
                  {restaurant.isOpen ? 'Open Now' : 'Closed'}
                </span>
              </div>
            </div>
            <div className="header-actions">
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setIsFormOpen(true)}
              >
                Edit
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
              >
                Delete
              </button>
              <button 
                className="btn btn-primary btn-lg"
                onClick={handleReservation}
              >
                Reserve a Table
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="detail-content">
        <div className="container-wide">
          <div className="detail-grid">
            {/* Left Column - Main Info */}
            <div className="detail-main">
              {/* About Section */}
              <section className="content-section">
                <h2 className="section-title">About</h2>
                <p className="section-text">{restaurant.description}</p>
              </section>

              {restaurant.menuImage && (
                <section className="content-section">
                  <h2 className="section-title">Menu</h2>
                  <a
                    href={resolveRestaurantMediaUrl(restaurant.menuImage)}
                    target="_blank"
                    rel="noreferrer"
                    className="menu-image-link"
                  >
                    <img
                      src={resolveRestaurantMediaUrl(restaurant.menuImage)}
                      alt={`${restaurant.name} menu`}
                      className="menu-image"
                    />
                  </a>
                </section>
              )}

              {/* Features */}
              {restaurant.features && restaurant.features.length > 0 && (
                <section className="content-section">
                  <h2 className="section-title">Amenities</h2>
                  <div className="features-grid">
                    {restaurant.features.map((feature, idx) => (
                      <div key={idx} className="feature-item">
                        <span className="feature-check">✓</span>
                        <span>{feature}</span>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              {/* Reviews Section */}
              <section className="content-section reviews-section">
                <div className="section-header-with-action">
                  <h2 className="section-title">Reviews</h2>
                  <button 
                    className="btn btn-outline btn-sm"
                    onClick={handleWriteReview}
                  >
                    Write a Review
                  </button>
                </div>

                {reviews.length === 0 ? (
                  <div className="empty-reviews">
                    <p>No reviews yet. Be the first to share your experience!</p>
                  </div>
                ) : (
                  <>
                    <div className="reviews-grid">
                      {reviews.slice(0, 6).map(review => (
                        <div key={review.id} className="review-item">
                          <div className="review-header-info">
                            <div className="reviewer-avatar-wrapper">
                              <img 
                                src={review.userAvatar || 'https://via.placeholder.com/48'} 
                                alt={review.userName}
                                className="reviewer-avatar"
                              />
                            </div>
                            <div className="reviewer-details">
                              <h4 className="reviewer-name">{review.userName}</h4>
                              <p className="review-date">
                                {new Date(review.createdAt).toLocaleDateString('en-US', { 
                                  month: 'short', 
                                  day: 'numeric', 
                                  year: 'numeric' 
                                })}
                              </p>
                            </div>
                            <div className="review-rating-display">
                              {review.rating.toFixed(1)} ★
                            </div>
                          </div>
                          {review.title && <h5 className="review-item-title">{review.title}</h5>}
                          <p className="review-text">{review.comment}</p>
                        </div>
                      ))}
                    </div>

                    {reviews.length > 6 && (
                      <div className="view-all-reviews">
                        <button 
                          className="btn btn-outline"
                          onClick={() => navigate('/reviews', { state: { restaurantId: id } })}
                        >
                          View All {reviews.length} Reviews
                        </button>
                      </div>
                    )}
                  </>
                )}
              </section>
            </div>

            {/* Right Column - Sidebar Info */}
            <aside className="detail-sidebar">
              <div className="info-card">
                <h3 className="info-card-title">Information</h3>
                
                <div className="info-list">
                  <div className="info-row">
                    <span className="info-icon">📍</span>
                    <div className="info-content">
                      <span className="info-label">Address</span>
                      <span className="info-text">{restaurant.address}</span>
                    </div>
                  </div>

                  <div className="info-row">
                    <span className="info-icon">📞</span>
                    <div className="info-content">
                      <span className="info-label">Phone</span>
                      <span className="info-text">{restaurant.phone}</span>
                    </div>
                  </div>

                  <div className="info-row">
                    <span className="info-icon">🕐</span>
                    <div className="info-content">
                      <span className="info-label">Hours</span>
                      <span className="info-text">{restaurant.openingHours}</span>
                    </div>
                  </div>

                  <div className="info-row">
                    <span className="info-icon">💳</span>
                    <div className="info-content">
                      <span className="info-label">Price Range</span>
                      <span className="info-text">{restaurant.priceRange}</span>
                    </div>
                  </div>
                </div>

                <button 
                  className="btn btn-primary btn-block"
                  onClick={handleReservation}
                >
                  Reserve a Table
                </button>
              </div>
            </aside>
          </div>
        </div>
      </div>

      {isFormOpen && restaurant && (
        <RestaurantFormModal
          restaurant={restaurant}
          onClose={() => setIsFormOpen(false)}
          onSaved={(saved) => setRestaurant(saved)}
        />
      )}
    </div>
  );
};

export default RestaurantDetailPage;
