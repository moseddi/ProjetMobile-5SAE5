import { useEffect, useState } from 'react';
import { reviewService } from '../../services/review/reviewService';
import { Review } from '../../types';
import './ReviewsPage.css';

const ReviewsPage = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [filteredReviews, setFilteredReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState('all');

  useEffect(() => {
    loadReviews();
  }, []);

  useEffect(() => {
    filterReviews();
  }, [reviews, selectedRating]);

  const loadReviews = async () => {
    try {
      const data = await reviewService.getAll();
      setReviews(data);
      setFilteredReviews(data);
    } catch (error) {
      console.error('Failed to load reviews:', error);
    } finally {
      setLoading(false);
    }
  };

  const filterReviews = () => {
    let filtered = [...reviews];

    if (selectedRating !== 'all') {
      filtered = filtered.filter(r => r.rating === parseInt(selectedRating));
    }

    setFilteredReviews(filtered);
  };

  return (
    <div className="reviews-page">
      <div className="page-header">
        <div className="container">
          <h1>Customer Reviews</h1>
          <p>See what others are saying about their dining experiences</p>
        </div>
      </div>

      <div className="container">
        <div className="reviews-layout">
          {/* Filters */}
          <aside className="filters-sidebar">
            <div className="filter-section">
              <h3>Filter by Rating</h3>
              <div className="rating-filters">
                <button
                  className={`rating-filter-btn ${selectedRating === 'all' ? 'active' : ''}`}
                  onClick={() => setSelectedRating('all')}
                >
                  All Ratings
                </button>
                {[5, 4, 3, 2, 1].map(rating => (
                  <button
                    key={rating}
                    className={`rating-filter-btn ${selectedRating === rating.toString() ? 'active' : ''}`}
                    onClick={() => setSelectedRating(rating.toString())}
                  >
                    {'⭐'.repeat(rating)} {rating} Stars
                  </button>
                ))}
              </div>
            </div>

            <button
              className="btn-clear-filters"
              onClick={() => setSelectedRating('all')}
            >
              Clear Filters
            </button>
          </aside>

          {/* Reviews List */}
          <div className="reviews-content">
            <div className="results-header">
              <h2>
                {filteredReviews.length} {filteredReviews.length === 1 ? 'Review' : 'Reviews'}
              </h2>
            </div>

            {loading ? (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Loading reviews...</p>
              </div>
            ) : filteredReviews.length === 0 ? (
              <div className="empty-state">
                <p>No reviews found</p>
              </div>
            ) : (
              <div className="reviews-list">
                {filteredReviews.map(review => (
                  <div key={review.id} className="review-card">
                    <div className="review-main">
                      <div className="reviewer-section">
                        <img
                          src={review.userAvatar || 'https://via.placeholder.com/64'}
                          alt={review.userName}
                          className="reviewer-avatar"
                        />
                        <div className="reviewer-details">
                          <h4>{review.userName}</h4>
                          <p className="review-date">
                            {new Date(review.createdAt).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric'
                            })}
                          </p>
                        </div>
                      </div>

                      <div className="review-content">
                        <div className="review-header">
                          <div className="review-rating">
                            {'⭐'.repeat(review.rating)}
                          </div>
                          {review.title && <h3 className="review-title">{review.title}</h3>}
                        </div>

                        <p className="review-comment">{review.comment}</p>

                        {review.photoUrl && (
                          <img
                            src={review.photoUrl}
                            alt="Review"
                            className="review-photo"
                          />
                        )}
                      </div>
                    </div>

                    <div className="restaurant-reference">
                      <div className="restaurant-info">
                        <img
                          src={review.restaurantImage}
                          alt={review.restaurantName}
                          className="restaurant-thumb"
                        />
                        <div>
                          <span className="restaurant-label">Restaurant</span>
                          <h5 className="restaurant-name">{review.restaurantName}</h5>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReviewsPage;
