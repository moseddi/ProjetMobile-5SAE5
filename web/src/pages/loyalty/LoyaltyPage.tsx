import './LoyaltyPage.css';

const LoyaltyPage = () => {

  // Mock loyalty data - Replace with actual API call
  const loyaltyData = {
    points: 2450,
    tier: 'Gold',
    nextTier: 'Platinum',
    pointsToNextTier: 550,
    rewardsAvailable: 3
  };

  const rewards = [
    {
      id: '1',
      title: '$10 Dining Credit',
      points: 500,
      description: 'Redeem for $10 off your next reservation'
    },
    {
      id: '2',
      title: 'Free Appetizer',
      points: 750,
      description: 'Complimentary appetizer at participating restaurants'
    },
    {
      id: '3',
      title: '$25 Dining Credit',
      points: 1000,
      description: 'Redeem for $25 off your next reservation'
    }
  ];

  return (
    <div className="loyalty-page">
      {/* Hero Section */}
      <section className="loyalty-hero">
        <div className="container-wide">
          <div className="loyalty-hero-content">
            <h1 className="loyalty-hero-title">
              Your <span className="accent-text">Rewards</span>
            </h1>
            <p className="loyalty-hero-subtitle">
              Earn points with every reservation and unlock exclusive benefits
            </p>
          </div>
        </div>
      </section>

      <div className="container-wide">
        {/* Points Overview */}
        <section className="loyalty-overview">
          <div className="points-card">
            <div className="points-main">
              <div className="points-number">{loyaltyData.points.toLocaleString()}</div>
              <div className="points-label">Available Points</div>
            </div>
            <div className="tier-badge">{loyaltyData.tier} Member</div>
          </div>

          <div className="progress-card">
            <div className="progress-header">
              <h3>Progress to {loyaltyData.nextTier}</h3>
              <span className="progress-remaining">{loyaltyData.pointsToNextTier} points to go</span>
            </div>
            <div className="progress-bar-container">
              <div 
                className="progress-bar-fill" 
                style={{ width: `${(loyaltyData.points / (loyaltyData.points + loyaltyData.pointsToNextTier)) * 100}%` }}
              ></div>
            </div>
          </div>
        </section>

        {/* Rewards Grid */}
        <section className="rewards-section">
          <h2 className="section-title-loyalty">Available Rewards</h2>
          <div className="rewards-grid">
            {rewards.map((reward) => (
              <div key={reward.id} className="reward-card">
                <div className="reward-content">
                  <h3 className="reward-title">{reward.title}</h3>
                  <p className="reward-description">{reward.description}</p>
                  <div className="reward-points">{reward.points} points</div>
                </div>
                <button 
                  className="btn btn-outline btn-sm"
                  disabled={loyaltyData.points < reward.points}
                >
                  {loyaltyData.points >= reward.points ? 'Redeem' : 'Not enough points'}
                </button>
              </div>
            ))}
          </div>
        </section>

        {/* How it Works */}
        <section className="how-it-works">
          <h2 className="section-title-loyalty">How It Works</h2>
          <div className="steps-grid">
            <div className="step-item">
              <div className="step-number">1</div>
              <h3>Make Reservations</h3>
              <p>Earn points every time you book and dine at partner restaurants</p>
            </div>
            <div className="step-item">
              <div className="step-number">2</div>
              <h3>Collect Points</h3>
              <p>Build up your balance and unlock higher membership tiers</p>
            </div>
            <div className="step-item">
              <div className="step-number">3</div>
              <h3>Redeem Rewards</h3>
              <p>Use points for dining credits, exclusive experiences, and more</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default LoyaltyPage;
