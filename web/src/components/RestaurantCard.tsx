import { Link } from 'react-router-dom';
import { Restaurant } from '../types';
import './RestaurantCard.css';

interface RestaurantCardProps {
  restaurant: Restaurant;
}

const RestaurantCard = ({ restaurant }: RestaurantCardProps) => {
  const formatPriceRange = (range: string) => {
    const priceMap = {
      'BUDGET': '$',
      'MODERATE': '$$',
      'EXPENSIVE': '$$$',
      'LUXURY': '$$$$'
    };
    return priceMap[range as keyof typeof priceMap] || '$$';
  };

  return (
    <Link to={`/restaurants/${restaurant.id}`} className="restaurant-card">
      <div className="restaurant-card-image">
        <img src={restaurant.image} alt={restaurant.name} />
        {!restaurant.isOpen && (
          <div className="closed-badge">Closed</div>
        )}
        <div className="rating-badge">
          ⭐ {restaurant.rating.toFixed(1)}
        </div>
      </div>
      
      <div className="restaurant-card-content">
        <h3 className="restaurant-name">{restaurant.name}</h3>
        
        <div className="restaurant-meta">
          <span className="cuisine-badge">{restaurant.cuisine}</span>
          <span className="price-range">{formatPriceRange(restaurant.priceRange)}</span>
        </div>
        
        <p className="restaurant-description">{restaurant.description}</p>
        
        <div className="restaurant-footer">
          <div className="review-count">
            {restaurant.reviewCount} reviews
          </div>
          {restaurant.features && restaurant.features.length > 0 && (
            <div className="features-preview">
              {restaurant.features.slice(0, 2).map((feature, idx) => (
                <span key={idx} className="feature-tag">{feature}</span>
              ))}
            </div>
          )}
        </div>
      </div>
    </Link>
  );
};

export default RestaurantCard;
