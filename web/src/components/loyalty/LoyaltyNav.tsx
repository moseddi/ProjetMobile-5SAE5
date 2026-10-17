import { NavLink } from 'react-router-dom';
import './LoyaltyNav.css';

const LoyaltyNav = () => {
  const tabs = [
    { to: '/loyalty', label: 'Overview', end: true },
    { to: '/loyalty/games', label: 'Games' },
    { to: '/loyalty/rewards', label: 'Rewards' },
    { to: '/loyalty/history', label: 'History' },
    { to: '/loyalty/assistant', label: '✨ Assistant' },
  ];

  return (
    <nav className="loyalty-nav">
      <div className="container-wide loyalty-nav-inner">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) => `loyalty-nav-tab ${isActive ? 'active' : ''}`}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
};

export default LoyaltyNav;
