import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { loyaltyService } from '../../services/loyalty/loyaltyService';
import { LoyaltyPointTransaction, LoyaltyRedemption } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import './LoyaltyPage.css';
import './HistoryPage.css';

const HistoryPage = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<LoyaltyPointTransaction[]>([]);
  const [redemptions, setRedemptions] = useState<LoyaltyRedemption[]>([]);
  const [tab, setTab] = useState<'transactions' | 'redemptions'>('transactions');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    Promise.all([
      loyaltyService.getTransactions(user.id),
      loyaltyService.getRedemptions(user.id),
    ]).then(([tx, red]) => {
      setTransactions(tx);
      setRedemptions(red);
    }).finally(() => setLoading(false));
  }, [user?.id]);

  if (loading) {
    return (
      <div className="loyalty-page">
        <LoyaltyNav />
        <div className="loading"><div className="spinner"></div></div>
      </div>
    );
  }

  return (
    <div className="loyalty-page">
      <LoyaltyNav />
      <div className="container history-page">
        <h1 className="section-title-loyalty">History</h1>

        <div className="history-tabs">
          <button className={`history-tab ${tab === 'transactions' ? 'active' : ''}`} onClick={() => setTab('transactions')}>
            Points history
          </button>
          <button className={`history-tab ${tab === 'redemptions' ? 'active' : ''}`} onClick={() => setTab('redemptions')}>
            Redemptions
          </button>
        </div>

        {tab === 'transactions' ? (
          <div className="history-list">
            {transactions.length === 0 && <p>No transactions yet.</p>}
            {transactions.map((tx) => (
              <div key={tx.id} className="history-row">
                <div>
                  <p className="history-row-title">{tx.description}</p>
                  <span className="history-row-date">{new Date(tx.createdAt).toLocaleString()}</span>
                </div>
                <span className={`history-row-points ${tx.points >= 0 ? 'positive' : 'negative'}`}>
                  {tx.points >= 0 ? '+' : ''}{tx.points}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="history-list">
            {redemptions.length === 0 && <p>No redemptions yet.</p>}
            {redemptions.map((r) => (
              <div key={r.id} className="history-row">
                <div>
                  <p className="history-row-title">{r.rewardName}</p>
                  <span className="history-row-date">{new Date(r.createdAt).toLocaleString()} · {r.status}</span>
                </div>
                <span className="history-row-points negative">-{r.pointsCost}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default HistoryPage;
