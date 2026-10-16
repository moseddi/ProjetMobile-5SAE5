import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import RestaurantFormModal from '../../components/RestaurantFormModal';
import { restaurantService } from '../../services/restaurant/restaurantService';
import { Restaurant } from '../../types';
import './RestaurantsPage.css';

const RestaurantsPage = () => {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [filteredRestaurants, setFilteredRestaurants] = useState<Restaurant[]>([]);
  const [featuredRestaurant, setFeaturedRestaurant] = useState<Restaurant | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCuisine, setSelectedCuisine] = useState('All');
  const [onlyOpenNow, setOnlyOpenNow] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRestaurant, setEditingRestaurant] = useState<Restaurant | null>(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    loadRestaurants();
  }, []);

  useEffect(() => {
    filterRestaurants();
  }, [restaurants, searchQuery, selectedCuisine, onlyOpenNow]);

  useEffect(() => {
    if (restaurants.length === 0) {
      setFeaturedRestaurant(null);
      return;
    }
    setFeaturedRestaurant(
      restaurants.reduce((prev, current) =>
        ((current.rating ?? 0) > (prev.rating ?? 0)) ? current : prev
      )
    );
  }, [restaurants]);

  const loadRestaurants = async () => {
    try {
      const data = await restaurantService.getAll();
      const list = Array.isArray(data) ? data : [];
      setRestaurants(list);
      setLoadError('');
    } catch (error) {
      console.error('Failed to load restaurants:', error);
      setRestaurants([]);
      setLoadError('The backend is not running. Start it with mvn spring-boot:run in the backend folder, then refresh this page.');
    } finally {
      setLoading(false);
    }
  };

  const filterRestaurants = () => {
    let filtered = [...restaurants];

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(r =>
        (r.name || '').toLowerCase().includes(query) ||
        (r.cuisine || '').toLowerCase().includes(query) ||
        (r.description || '').toLowerCase().includes(query)
      );
    }

    if (selectedCuisine !== 'All') {
      filtered = filtered.filter(r => r.cuisine === selectedCuisine);
    }

    if (onlyOpenNow) {
      filtered = filtered.filter(r => r.isOpen);
    }

    setFilteredRestaurants(filtered);
  };

  const handleRestaurantSaved = (saved: Restaurant) => {
    setRestaurants((prev) => {
      const exists = prev.some((item) => item.id === saved.id);
      return exists ? prev.map((item) => (item.id === saved.id ? saved : item)) : [saved, ...prev];
    });
  };

  const handleEdit = (restaurant: Restaurant) => {
    setEditingRestaurant(restaurant);
    setIsFormOpen(true);
  };

  const handleDelete = async (restaurant: Restaurant) => {
    if (!window.confirm(`Delete "${restaurant.name}"?`)) {
      return;
    }

    try {
      await restaurantService.remove(restaurant.id);
      setRestaurants((prev) => prev.filter((item) => item.id !== restaurant.id));
      toast.success('Restaurant deleted');
    } catch (error) {
      console.error('Failed to delete restaurant:', error);
    }
  };

  const cuisines = ['All', ...new Set(restaurants.map(r => r.cuisine))];

  if (loading) {
    return (
      <div className="restaurants-page">
        <div className="loading">
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="restaurants-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="container-wide">
          <div className="hero-content">
            <h1 className="hero-title">
              Discover your next <span className="accent-text">favorite table</span>
            </h1>
            <p className="hero-subtitle">
              Experience exceptional dining at the finest restaurants in your city
            </p>
            
            <div className="search-container">
              <input
                type="text"
                className="search-input-large"
                placeholder="Search restaurants, cuisines or locations..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <button
                type="button"
                className="btn btn-primary add-restaurant-btn"
                onClick={() => {
                  setEditingRestaurant(null);
                  setIsFormOpen(true);
                }}
              >
                Add restaurant
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Restaurant */}
      {featuredRestaurant && !searchQuery && selectedCuisine === 'All' && (
        <section className="featured-section">
          <div className="container-wide">
            <div className="section-label">Featured</div>
            <Link to={`/restaurants/${featuredRestaurant.id}`} className="featured-restaurant-card">
              <div className="featured-image-container">
                <img src={featuredRestaurant.image} alt={featuredRestaurant.name} />
                <div className="featured-overlay">
                  <div className="featured-badge">Chef's Selection</div>
                </div>
              </div>
              <div className="featured-info">
                <h2 className="featured-name">{featuredRestaurant.name}</h2>
                <div className="featured-meta">
                  <span className="cuisine-badge">{featuredRestaurant.cuisine}</span>
                  <span className="rating-large">
                    <span className="rating-value">{(featuredRestaurant.rating ?? 0).toFixed(1)}</span>
                    <span className="rating-stars">★</span>
                  </span>
                </div>
                <p className="featured-description">{featuredRestaurant.description}</p>
              </div>
            </Link>
          </div>
        </section>
      )}

      {/* Cuisine Filter */}
      <section className="filter-section">
        <div className="container-wide">
          <div className="filter-header">
            <h3 className="filter-title">Browse by Cuisine</h3>
          </div>
          <div className="cuisine-filters">
            {cuisines.map((cuisine) => (
              <button
                key={cuisine}
                className={`cuisine-btn ${selectedCuisine === cuisine ? 'active' : ''}`}
                onClick={() => setSelectedCuisine(cuisine)}
              >
                {cuisine}
              </button>
            ))}
          </div>
          
          <label className="filter-checkbox-label">
            <input
              type="checkbox"
              checked={onlyOpenNow}
              onChange={(e) => setOnlyOpenNow(e.target.checked)}
              className="filter-checkbox"
            />
            <span>Open now only</span>
          </label>
        </div>
      </section>

      {/* Restaurant Grid */}
      <section className="restaurants-section">
        <div className="container-wide">
          {loadError ? (
            <div className="empty-state">
              <h3>Cannot load restaurants</h3>
              <p>{loadError}</p>
              <button className="btn btn-primary" onClick={() => { setLoading(true); loadRestaurants(); }}>
                Retry
              </button>
            </div>
          ) : filteredRestaurants.length === 0 ? (
            <div className="empty-state">
              <h3>No restaurants found</h3>
              <p>Try adjusting your filters or search terms</p>
              <button 
                className="btn btn-primary"
                onClick={() => {
                  setSearchQuery('');
                  setSelectedCuisine('All');
                  setOnlyOpenNow(false);
                }}
              >
                Clear filters
              </button>
            </div>
          ) : (
            <div className="restaurant-grid">
              {filteredRestaurants.map((restaurant) => (
                <article key={restaurant.id} className="restaurant-card">
                  <Link to={`/restaurants/${restaurant.id}`} className="restaurant-card-link">
                    <div className="restaurant-image-wrapper">
                      <img src={restaurant.image} alt={restaurant.name} className="restaurant-image" />
                      {!restaurant.isOpen && (
                        <div className="closed-badge">Closed</div>
                      )}
                    </div>
                    <div className="restaurant-card-content">
                      <h3 className="restaurant-name">{restaurant.name}</h3>
                      <p className="restaurant-cuisine">{restaurant.cuisine}</p>
                      <div className="restaurant-card-footer">
                        <span className="restaurant-rating">
                          {(restaurant.rating ?? 0).toFixed(1)} ★
                        </span>
                        <span className="restaurant-price">{restaurant.priceRange}</span>
                      </div>
                    </div>
                  </Link>
                  <div className="restaurant-card-actions">
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => handleEdit(restaurant)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDelete(restaurant)}
                    >
                      Delete
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      {isFormOpen && (
        <RestaurantFormModal
          restaurant={editingRestaurant}
          onClose={() => {
            setIsFormOpen(false);
            setEditingRestaurant(null);
          }}
          onSaved={handleRestaurantSaved}
        />
      )}
    </div>
  );
};

export default RestaurantsPage;
