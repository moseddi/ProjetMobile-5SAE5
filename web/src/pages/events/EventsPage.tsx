import './EventsPage.css';

const EventsPage = () => {
  // Mock events data - Replace with actual API call
  const events = [
    {
      id: '1',
      title: 'Wine Tasting Evening',
      restaurant: 'Le Bernardin',
      date: '2024-02-20',
      time: '18:00',
      image: 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=800&q=80',
      description: 'Experience an exclusive wine tasting featuring rare vintages',
      price: '$85 per person'
    },
    {
      id: '2',
      title: 'Chef\'s Table Experience',
      restaurant: 'Eleven Madison Park',
      date: '2024-02-25',
      time: '19:30',
      image: 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=80',
      description: 'An intimate dinner with our award-winning chef',
      price: '$250 per person'
    }
  ];

  return (
    <div className="events-page">
      {/* Hero Section */}
      <section className="events-hero">
        <div className="container-wide">
          <div className="hero-content-events">
            <h1 className="hero-title-events">
              Exclusive <span className="accent-text">Culinary Events</span>
            </h1>
            <p className="hero-subtitle-events">
              Join unforgettable dining experiences and special occasions
            </p>
          </div>
        </div>
      </section>

      {/* Events Grid */}
      <section className="events-section">
        <div className="container-wide">
          <div className="events-grid">
            {events.map((event) => (
              <div key={event.id} className="event-card">
                <div className="event-image-wrapper">
                  <img src={event.image} alt={event.title} className="event-image" />
                  <div className="event-date-badge">
                    <span className="event-month">
                      {new Date(event.date).toLocaleDateString('en-US', { month: 'short' })}
                    </span>
                    <span className="event-day">
                      {new Date(event.date).toLocaleDateString('en-US', { day: 'numeric' })}
                    </span>
                  </div>
                </div>
                <div className="event-content">
                  <h3 className="event-title">{event.title}</h3>
                  <p className="event-restaurant">{event.restaurant}</p>
                  <p className="event-description">{event.description}</p>
                  <div className="event-footer-info">
                    <div className="event-time">
                      <span className="event-icon">🕐</span>
                      <span>{event.time}</span>
                    </div>
                    <div className="event-price">{event.price}</div>
                  </div>
                  <button className="btn btn-primary btn-sm btn-full-mobile">
                    Reserve Spot
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
};

export default EventsPage;
