import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import './HangOutPage.css';

const HangOutPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleJoinHangout = (hangoutId: string) => {
    if (!user) {
      toast.info('Please sign in to join this hangout');
      navigate('/login', { state: { from: '/hangout', action: 'join', hangoutId } });
      return;
    }
    
    // TODO: Implement join hangout logic
    toast.success('Successfully joined hangout!');
  };

  const handleCreateHangout = () => {
    if (!user) {
      toast.info('Please sign in to create a hangout');
      navigate('/login', { state: { from: '/hangout', action: 'create' } });
      return;
    }
    
    // TODO: Navigate to create hangout page
    toast.info('Create hangout feature coming soon!');
  };

  // Mock data - Replace with actual API call
  const hangouts = [
    {
      id: '1',
      title: 'Dinner at Le Bernardin',
      restaurant: 'Le Bernardin',
      date: '2024-02-15',
      time: '19:00',
      creator: 'Sarah Johnson',
      participants: 4,
      maxParticipants: 8,
      description: 'Join us for an amazing seafood experience!'
    },
    {
      id: '2',
      title: 'Sunday Brunch Meetup',
      restaurant: 'The Ivy',
      date: '2024-02-18',
      time: '11:00',
      creator: 'Michael Chen',
      participants: 6,
      maxParticipants: 10,
      description: 'Casual brunch with great company'
    }
  ];

  return (
    <div className="hangout-page">
      {/* Hero Section */}
      <section className="hangout-hero">
        <div className="container-wide">
          <div className="hero-content-hangout">
            <h1 className="hero-title-hangout">
              Hang Out & <span className="accent-text">Dine Together</span>
            </h1>
            <p className="hero-subtitle-hangout">
              Discover dining companions and share memorable meals with new friends
            </p>
            <button className="btn btn-primary btn-lg" onClick={handleCreateHangout}>
              Create Hangout
            </button>
          </div>
        </div>
      </section>

      {/* Hangouts List */}
      <section className="hangouts-section">
        <div className="container-wide">
          <div className="section-header-hangout">
            <h2 className="section-title-hangout">Upcoming Hangouts</h2>
            <p className="section-subtitle-hangout">Browse and join dining experiences</p>
          </div>

          <div className="hangouts-grid">
            {hangouts.map((hangout) => (
              <div key={hangout.id} className="hangout-card">
                <div className="hangout-card-header">
                  <h3 className="hangout-title">{hangout.title}</h3>
                  <div className="hangout-restaurant">{hangout.restaurant}</div>
                </div>

                <div className="hangout-details">
                  <div className="hangout-detail-item">
                    <span className="detail-icon">📅</span>
                    <span>{new Date(hangout.date).toLocaleDateString('en-US', { 
                      weekday: 'long', 
                      month: 'short', 
                      day: 'numeric' 
                    })}</span>
                  </div>
                  <div className="hangout-detail-item">
                    <span className="detail-icon">🕐</span>
                    <span>{hangout.time}</span>
                  </div>
                  <div className="hangout-detail-item">
                    <span className="detail-icon">👥</span>
                    <span>{hangout.participants} / {hangout.maxParticipants} joined</span>
                  </div>
                </div>

                <p className="hangout-description">{hangout.description}</p>

                <div className="hangout-footer">
                  <div className="hangout-creator">
                    <span className="creator-label">Organized by</span>
                    <span className="creator-name">{hangout.creator}</span>
                  </div>
                  <button 
                    className="btn btn-outline btn-sm"
                    onClick={() => handleJoinHangout(hangout.id)}
                  >
                    Join Hangout
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

export default HangOutPage;
