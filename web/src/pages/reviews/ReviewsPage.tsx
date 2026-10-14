import { useEffect, useState, useRef, useCallback } from 'react';
import { reviewService } from '../../services/review/reviewService';
import { restaurantService } from '../../services/restaurant/restaurantService';
import { Review, Restaurant } from '../../types';
import ragData from '../../data/ragRestaurants.json';
import './ReviewsPage.css';

// ─── Types ───────────────────────────────────────────────────────────────────
interface RagRestaurant {
  restaurant: string;
  food: number;
  service: number;
  price: number;
  ambiance: number;
  rating: string;
}

interface Snippet {
  restaurant: string;
  text: string;
}

// ─── Auto-rating engine (keyword-based, scale 1 to 5) ────────────────────────
const POS_FOOD    = ['delicious','amazing food','great food','excellent food','tasty','flavorful','savoureux','délicieux','good food','best food','fresh','tender','authentic'];
const NEG_FOOD    = ['bad food','food was not good','food was terrible','bland','tasteless','cold food','disappointed by food','overcooked','raw','inedible','nourriture décevante','not good enough'];
const POS_SERVICE = ['great service','excellent service','fast service','attentive','friendly staff','waiter was great','courteous','polite','very helpful','super service','bon service','staff friendly'];
const NEG_SERVICE = ['slow service','rude','bad service','waiter was rude','ignored us','waited','long wait','poor service','unfriendly','inattentif','service trop lent','underperform'];
const POS_PRICE   = ['affordable','good value','cheap','reasonable price','worth it','cost effective','rapport qualité','not expensive','great deal','bon rapport'];
const NEG_PRICE   = ['expensive','overpriced','too pricey','too costly','cher','trop cher','extortionate','overcharge','not worth','bill was high','addition trop','way too much'];
const POS_AMB     = ['great ambiance','beautiful','cozy','lovely atmosphere','nice decor','warm atmosphere','pleasant','relaxing','romantic','wonderful setting','beau cadre','belle ambiance'];
const NEG_AMB     = ['no ambiance','bad ambiance','loud','noisy','uncomfortable','dirty','ugly decor','not comfortable','poor atmosphere','ambiance décevante','pas d\'ambiance','disappointing ambiance'];

function scoreFromKeywords(text: string, pos: string[], neg: string[]): number | null {
  const t = text.toLowerCase();
  let hasPos = pos.some(w => t.includes(w));
  let hasNeg = neg.some(w => t.includes(w));
  
  if (hasNeg) return 1;
  if (hasPos) return 5;
  return null; // no keyword detected -> don't force change
}

function autoRate(text: string) {
  return {
    food:     scoreFromKeywords(text, POS_FOOD,    NEG_FOOD),
    service:  scoreFromKeywords(text, POS_SERVICE, NEG_SERVICE),
    price:    scoreFromKeywords(text, POS_PRICE,   NEG_PRICE),
    ambiance: scoreFromKeywords(text, POS_AMB,     NEG_AMB),
  };
}

// ─── Recommendation engine (pure client-side, no server) ────────────────────
function getRecommendations(issue: 'food' | 'service' | 'price' | 'ambiance', excludeName: string): RagRestaurant[] {
  return (ragData.restaurants as RagRestaurant[])
    .filter(r => r.restaurant.toLowerCase() !== excludeName.toLowerCase())
    .filter(r => r[issue] >= 7)
    .sort((a, b) => b[issue] - a[issue])
    .slice(0, 3);
}

// Helper to convert /10 score from Chroma dataset to /5 scale
function toFiveScale(scoreOutOfTen: number): string {
  return (scoreOutOfTen / 2).toFixed(1);
}

// ─── Component ───────────────────────────────────────────────────────────────
const ReviewsPage = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [filteredReviews, setFilteredReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRating, setSelectedRating] = useState('all');

  // Form states
  const [selectedRestaurantId, setSelectedRestaurantId] = useState('');
  const [reviewTitle, setReviewTitle] = useState('');
  const [comment, setComment] = useState('');
  const [foodRating, setFoodRating] = useState(3);
  const [serviceRating, setServiceRating] = useState(3);
  const [priceRating, setPriceRating] = useState(3);
  const [ambianceRating, setAmbianceRating] = useState(3);
  
  // Track which sliders the user manually touched so auto-rating NEVER overrides manual user input
  const [touched, setTouched] = useState({
    food: false,
    service: false,
    price: false,
    ambiance: false
  });

  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState(false);
  const [alternatives, setAlternatives] = useState<RagRestaurant[]>([]);
  const [issueTitle, setIssueTitle] = useState('');
  const [activeIssueCriteria, setActiveIssueCriteria] = useState<'food' | 'service' | 'price' | 'ambiance'>('service');

  // Autocomplete states
  const [suggestions, setSuggestions] = useState<Snippet[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  useEffect(() => { loadData(); }, []);
  useEffect(() => { filterReviews(); }, [reviews, selectedRating]);

  // Close suggestions when clicking outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (suggestionsRef.current && !suggestionsRef.current.contains(e.target as Node)
        && textareaRef.current && !textareaRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const loadData = async () => {
    try {
      const [reviewData, restaurantData] = await Promise.all([
        reviewService.getAll().catch(() => []),
        restaurantService.getAll().catch(() => [])
      ]);

      const ragDatasetRestaurants: Restaurant[] = [
        { id: 'rag_1', name: 'Beyond Flavours', cuisine: 'North Indian / Asian', priceRange: 'MODERATE', rating: 4.8, reviewCount: 142, isOpen: true, address: 'Plot 36, HITEC City', phone: '+91 40 6815 6789', openingHours: '12:00 PM - 11:30 PM', description: 'Renommé pour son ambiance et son service courtois.', image: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&fit=crop' },
        { id: 'rag_2', name: 'Paradise', cuisine: 'Biryani / Mughlai', priceRange: 'MODERATE', rating: 4.5, reviewCount: 230, isOpen: true, address: 'SD Road, Secunderabad', phone: '+91 40 2784 3115', openingHours: '11:00 AM - 11:00 PM', description: 'Institution légendaire pour la gastronomie.', image: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400&fit=crop' },
        { id: 'rag_3', name: 'Over The Moon Brew Company', cuisine: 'Continental / Finger Food', priceRange: 'EXPENSIVE', rating: 4.6, reviewCount: 95, isOpen: true, address: 'Daspalla Hotel, Jubilee Hills', phone: '+91 40 6684 4444', openingHours: '01:00 PM - 12:00 AM', description: 'Brasserie haut de gamme et vue panoramique.', image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=400&fit=crop' },
        { id: 'rag_4', name: 'Flechazo', cuisine: 'Mediterranean / Asian', priceRange: 'MODERATE', rating: 4.2, reviewCount: 110, isOpen: true, address: 'Madhapur Main Road', phone: '+91 40 3316 5233', openingHours: '11:30 AM - 11:00 PM', description: 'Buffet méditerranéen et expérience festive.', image: 'https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?w=400&fit=crop' },
        { id: 'rag_5', name: "The Fisherman's Wharf", cuisine: 'Seafood / Goan', priceRange: 'EXPENSIVE', rating: 4.4, reviewCount: 88, isOpen: true, address: 'Financial District, Gachibowli', phone: '+91 40 6733 1133', openingHours: '12:00 PM - 11:00 PM', description: 'Spécialités de poissons et cadre tropical.', image: 'https://images.unsplash.com/photo-1537047902294-62a40c20a6ae?w=400&fit=crop' },
        { id: 'rag_6', name: 'Sushi Zen', cuisine: 'Japanese / Sushi Bar', priceRange: 'EXPENSIVE', rating: 4.7, reviewCount: 78, isOpen: true, address: 'Little Tokyo, Downtown', phone: '+33 1 42 68 00 11', openingHours: '12:00 PM - 10:30 PM', description: "Poissons d'une fraîcheur exceptionnelle.", image: 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=400&fit=crop' },
        { id: 'rag_7', name: 'Shah Ghouse Hotel & Restaurant', cuisine: 'Hyderabadi / Biryani', priceRange: 'BUDGET', rating: 4.3, reviewCount: 310, isOpen: true, address: 'Tolichowki, Hyderabad', phone: '+91 40 2354 1234', openingHours: '11:00 AM - 12:00 AM', description: 'Haleem et biryani légendaires.', image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400&fit=crop' },
      ];

      const combined = [...restaurantData, ...ragDatasetRestaurants.filter(r => !restaurantData.some(b => b.name === r.name))];
      setReviews(reviewData);
      setFilteredReviews(reviewData);
      setRestaurants(combined);
      if (combined.length > 0) setSelectedRestaurantId(combined[0].id);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const filterReviews = () => {
    let filtered = [...reviews];
    if (selectedRating !== 'all') filtered = filtered.filter(r => r.rating === parseInt(selectedRating));
    setFilteredReviews(filtered);
  };

  // ── Auto-rating: updates ONLY untouched sliders as you type ────────────────
  const handleCommentChange = useCallback((text: string) => {
    setComment(text);

    if (text.length >= 3) {
      const rated = autoRate(text);
      
      // ONLY update sliders that the user HAS NOT manually moved
      if (!touched.food && rated.food !== null) setFoodRating(rated.food);
      if (!touched.service && rated.service !== null) setServiceRating(rated.service);
      if (!touched.price && rated.price !== null) setPriceRating(rated.price);
      if (!touched.ambiance && rated.ambiance !== null) setAmbianceRating(rated.ambiance);
    }

    // Autocomplete: filter snippets matching first typed word
    if (text.length >= 1) {
      const firstWord = text.trim().split(/\s+/)[0].toLowerCase();
      const matched = (ragData.snippets as Snippet[]).filter(s =>
        s.text.toLowerCase().includes(firstWord)
      ).slice(0, 5);
      setSuggestions(matched);
      setShowSuggestions(matched.length > 0);
    } else {
      setShowSuggestions(false);
    }
  }, [touched]);

  const applySuggestion = (snippet: Snippet) => {
    setComment(snippet.text);
    handleCommentChange(snippet.text);
    setShowSuggestions(false);
    const match = restaurants.find(r => r.name.toLowerCase() === snippet.restaurant.toLowerCase());
    if (match) setSelectedRestaurantId(match.id);
  };

  // Manual slider handlers — mark touched as true so auto-rating never overwrites
  const handleSliderChange = (criterion: 'food' | 'service' | 'price' | 'ambiance', val: number) => {
    setTouched(prev => ({ ...prev, [criterion]: true }));
    if (criterion === 'food') setFoodRating(val);
    if (criterion === 'service') setServiceRating(val);
    if (criterion === 'price') setPriceRating(val);
    if (criterion === 'ambiance') setAmbianceRating(val);
  };

  // ── Submit ────────────────────────────────────────────────────────────────
  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    const chosenResto = restaurants.find(r => r.id === selectedRestaurantId);
    const restoName = chosenResto ? chosenResto.name : 'Restaurant';
    if (!comment.trim()) return;

    setSubmitting(true);
    setSuccessMessage(false);
    setAlternatives([]);
    setIssueTitle('');

    const overallRating = Math.round((foodRating + serviceRating + priceRating + ambianceRating) / 4);

    const newReviewObj: Review = {
      id: `rev_${Date.now()}`,
      userId: 'u_current',
      userName: 'Vous (Avis récent)',
      userAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop',
      restaurantId: selectedRestaurantId,
      restaurantName: restoName,
      restaurantImage: chosenResto?.image || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=100&h=100&fit=crop',
      rating: overallRating,
      title: reviewTitle || 'Expérience culinaire',
      comment: comment,
      createdAt: new Date().toISOString()
    };

    setReviews(prev => [newReviewObj, ...prev]);
    setFilteredReviews(prev => [newReviewObj, ...prev]);
    setSuccessMessage(true);

    try {
      const { aiService } = await import('../../services/ai/aiService');
      aiService.analyzeReview(restoName, comment, overallRating.toString()).catch(() => {});
    } catch (_) {}

    // Smart recommendation trigger
    const minScore = Math.min(foodRating, serviceRating, priceRating, ambianceRating);
    if (minScore <= 2) {
      let worstCriteria: 'food' | 'service' | 'price' | 'ambiance' = 'service';
      let title = "💡 Déçu par l'accueil ? Voici des établissements réputés pour leur service attentionné :";

      if (priceRating === minScore) {
        worstCriteria = 'price';
        title = "💡 L'addition était trop élevée ? Voici des alternatives avec un excellent rapport qualité/prix :";
      } else if (foodRating === minScore) {
        worstCriteria = 'food';
        title = "💡 Déçu par la cuisine ? Voici des restaurants réputés pour l'excellence de leurs plats :";
      } else if (ambianceRating === minScore) {
        worstCriteria = 'ambiance';
        title = "💡 L'ambiance était décevante ? Voici des établissements avec un cadre agréable et chaleureux :";
      }

      setActiveIssueCriteria(worstCriteria);
      const recs = getRecommendations(worstCriteria, restoName);
      if (recs.length > 0) {
        setAlternatives(recs);
        setIssueTitle(title);
      }
    }

    // Reset form cleanly
    setComment('');
    setReviewTitle('');
    setFoodRating(3);
    setServiceRating(3);
    setPriceRating(3);
    setAmbianceRating(3);
    setTouched({ food: false, service: false, price: false, ambiance: false });
    setSubmitting(false);
  };

  // ─── Render ───────────────────────────────────────────────────────────────
  return (
    <div className="reviews-page">
      <div className="page-header">
        <div className="container">
          <h1>Customer Reviews</h1>
          <p>See what others are saying about their dining experiences</p>
        </div>
      </div>

      <div className="container">
        {/* ── Review Form ── */}
        <div className="review-card" style={{ marginBottom: 'var(--space-2xl)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <span style={{ fontSize: '1.5rem' }}>✍️</span>
            <div>
              <h3 style={{ margin: 0, fontFamily: 'Playfair Display, serif', fontSize: '1.4rem', color: 'var(--color-text-primary)' }}>
                Rédiger un avis détaillé
              </h3>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
                Donnez vos notes ou décrivez votre expérience pour une auto-évaluation ✨
              </p>
            </div>
          </div>

          <form onSubmit={handleSubmitReview}>
            {/* Restaurant selector */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '6px', color: 'var(--color-text-primary)' }}>
                Choisir un restaurant
              </label>
              <select
                value={selectedRestaurantId}
                onChange={(e) => setSelectedRestaurantId(e.target.value)}
                style={{ width: '100%', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1.5px solid var(--color-border)', background: 'white', fontSize: '0.9375rem', fontFamily: 'Inter, sans-serif' }}
              >
                {restaurants.length > 0 ? (
                  restaurants.map(r => (
                    <option key={r.id} value={r.id}>{r.name} — {r.cuisine} ({r.priceRange})</option>
                  ))
                ) : (
                  <>
                    <option value="r1">Beyond Flavours — Indian, Multi-Cuisine (€€)</option>
                    <option value="r2">Paradise — Biryani, Mughlai & Kebab (€€)</option>
                  </>
                )}
              </select>
            </div>

            {/* 4-criteria sliders — 1 to 5 stars */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', padding: '16px', background: 'var(--color-bg-secondary)', borderRadius: 'var(--radius-sm)', marginBottom: '16px' }}>
              {([
                { key: 'food',     label: '🍽️ Cuisine',      value: foodRating },
                { key: 'service',  label: '🤝 Service',       value: serviceRating },
                { key: 'price',    label: '💶 Prix / Valeur', value: priceRating },
                { key: 'ambiance', label: '🏠 Ambiance',      value: ambianceRating },
              ] as const).map(({ key, label, value }) => (
                <div key={key}>
                  <label style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '6px' }}>
                    <span>{label}</span>
                    <span style={{ color: 'var(--color-primary)' }}>{'⭐'.repeat(value)} ({value}/5)</span>
                  </label>
                  <input
                    type="range" min="1" max="5" value={value}
                    onChange={(e) => handleSliderChange(key, parseInt(e.target.value))}
                    style={{ width: '100%' }}
                  />
                </div>
              ))}
            </div>

            {/* Title */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '6px', color: 'var(--color-text-primary)' }}>
                Titre de l'avis
              </label>
              <input
                type="text"
                placeholder="Ex : Superbe cuisine mais service trop lent"
                value={reviewTitle}
                onChange={(e) => setReviewTitle(e.target.value)}
                style={{ width: '100%', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1.5px solid var(--color-border)', fontSize: '0.9375rem', marginBottom: '14px', boxSizing: 'border-box' }}
              />

              {/* Comment with autocomplete */}
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '6px', color: 'var(--color-text-primary)' }}>
                Votre commentaire d'expérience
              </label>
              <div style={{ position: 'relative' }}>
                <textarea
                  ref={textareaRef}
                  placeholder="Écrivez votre commentaire..."
                  value={comment}
                  onChange={(e) => handleCommentChange(e.target.value)}
                  onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                  rows={4}
                  style={{ width: '100%', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1.5px solid var(--color-border)', fontSize: '0.9375rem', fontFamily: 'Inter, sans-serif', boxSizing: 'border-box' }}
                  required
                />

                {/* Autocomplete dropdown */}
                {showSuggestions && (
                  <div
                    ref={suggestionsRef}
                    style={{
                      position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 50,
                      background: 'white', border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-sm)', boxShadow: '0 4px 16px rgba(0,0,0,0.12)',
                      maxHeight: '220px', overflowY: 'auto'
                    }}
                  >
                    <div style={{ padding: '6px 12px', fontSize: '0.75rem', color: 'var(--color-text-secondary)', borderBottom: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)' }}>
                      💡 Suggestions d'avis similaires
                    </div>
                    {suggestions.map((s, i) => (
                      <div
                        key={i}
                        onClick={() => applySuggestion(s)}
                        style={{
                          padding: '10px 14px', cursor: 'pointer', borderBottom: i < suggestions.length - 1 ? '1px solid var(--color-border)' : 'none',
                          transition: 'background 0.15s'
                        }}
                        onMouseEnter={e => (e.currentTarget.style.background = 'var(--color-bg-secondary)')}
                        onMouseLeave={e => (e.currentTarget.style.background = 'white')}
                      >
                        <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: '2px' }}>
                          📍 {s.restaurant}
                        </div>
                        <div style={{ fontSize: '0.8125rem', color: 'var(--color-text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {s.text}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="rating-filter-btn active"
              style={{ padding: '12px 24px', fontWeight: 600 }}
            >
              {submitting ? 'Traitement en cours...' : 'Publier mon avis'}
            </button>
          </form>

          {successMessage && (
            <p style={{ marginTop: '12px', color: '#15803d', fontSize: '0.875rem', fontWeight: 500 }}>
              ✓ Votre avis a été enregistré avec succès !
            </p>
          )}

          {/* Smart alternatives — converted to /5 scale */}
          {alternatives.length > 0 && (
            <div style={{ marginTop: '16px', padding: '16px', borderRadius: 'var(--radius-sm)', background: '#fefce8', border: '1px solid #fef08a' }}>
              <p style={{ margin: '0 0 10px 0', fontSize: '0.9375rem', color: '#854d0e', fontWeight: 600 }}>
                {issueTitle}
              </p>
              <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                {alternatives.map((alt, idx) => (
                  <div key={idx} style={{ background: 'white', padding: '10px 14px', borderRadius: '8px', border: '1px solid #fde047', fontSize: '0.875rem' }}>
                    <strong style={{ color: 'var(--color-text-primary)' }}>{alt.restaurant}</strong>
                    <div style={{ marginTop: '4px', color: 'var(--color-text-secondary)', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                      <span style={activeIssueCriteria === 'food' ? { fontWeight: 700, color: '#15803d' } : {}}>🍽️ Cuisine {toFiveScale(alt.food)}/5</span>
                      <span style={activeIssueCriteria === 'service' ? { fontWeight: 700, color: '#15803d' } : {}}>🤝 Service {toFiveScale(alt.service)}/5</span>
                      <span style={activeIssueCriteria === 'price' ? { fontWeight: 700, color: '#15803d' } : {}}>💶 Prix {toFiveScale(alt.price)}/5</span>
                      <span style={activeIssueCriteria === 'ambiance' ? { fontWeight: 700, color: '#15803d' } : {}}>🏠 Ambiance {toFiveScale(alt.ambiance)}/5</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ── Reviews List with Sidebar ── */}
        <div className="reviews-layout">
          <aside className="filters-sidebar">
            <div className="filter-section">
              <h3>Filter by Rating</h3>
              <div className="rating-filters">
                <button className={`rating-filter-btn ${selectedRating === 'all' ? 'active' : ''}`} onClick={() => setSelectedRating('all')}>
                  All Ratings
                </button>
                {[5, 4, 3, 2, 1].map(rating => (
                  <button key={rating} className={`rating-filter-btn ${selectedRating === rating.toString() ? 'active' : ''}`} onClick={() => setSelectedRating(rating.toString())}>
                    {'⭐'.repeat(rating)} {rating} Stars
                  </button>
                ))}
              </div>
            </div>
            <button className="btn-clear-filters" onClick={() => setSelectedRating('all')}>Clear Filters</button>
          </aside>

          <div className="reviews-content">
            <div className="results-header">
              <h2>{filteredReviews.length} {filteredReviews.length === 1 ? 'Review' : 'Reviews'}</h2>
            </div>

            {loading ? (
              <div className="loading-state"><div className="spinner"></div><p>Loading reviews...</p></div>
            ) : filteredReviews.length === 0 ? (
              <div className="empty-state"><p>No reviews found</p></div>
            ) : (
              <div className="reviews-list">
                {filteredReviews.map(review => (
                  <div key={review.id} className="review-card">
                    <div className="review-main">
                      <div className="reviewer-section">
                        <img src={review.userAvatar || 'https://via.placeholder.com/64'} alt={review.userName} className="reviewer-avatar" />
                        <div className="reviewer-details">
                          <h4>{review.userName}</h4>
                          <p className="review-date">
                            {new Date(review.createdAt).toLocaleDateString('fr-FR', { year: 'numeric', month: 'long', day: 'numeric' })}
                          </p>
                        </div>
                      </div>

                      <div className="review-content">
                        <div className="review-header">
                          <div className="review-rating">{'⭐'.repeat(review.rating)}</div>
                          {review.title && <h3 className="review-title">{review.title}</h3>}
                        </div>
                        <p className="review-comment">{review.comment}</p>
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '12px' }}>
                          {[
                            { icon: '🍽️', label: 'Cuisine', val: review.rating },
                            { icon: '🤝', label: 'Service', val: review.rating },
                            { icon: '💶', label: 'Prix',    val: review.rating },
                            { icon: '🏠', label: 'Ambiance',val: review.rating },
                          ].map(({ icon, label, val }) => (
                            <span key={label} style={{ fontSize: '0.75rem', background: 'var(--color-bg-secondary)', padding: '4px 10px', borderRadius: '20px', border: '1px solid var(--color-border)', color: 'var(--color-text-secondary)' }}>
                              {icon} {label} {val}/5
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="restaurant-reference">
                      <div className="restaurant-info">
                        <img src={review.restaurantImage || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=100&h=100&fit=crop'} alt={review.restaurantName} className="restaurant-thumb" />
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
