import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import { loyaltyService } from '../../services/loyalty/loyaltyService';
import { LoyaltyReward } from '../../types';
import LoyaltyNav from '../../components/loyalty/LoyaltyNav';
import './LoyaltyPage.css';
import './RewardsPage.css';

type SortBy = 'cost-asc' | 'cost-desc';

const RewardsPage = () => {
  const { user } = useAuth();
  const [rewards, setRewards] = useState<LoyaltyReward[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState('all');
  const [sortBy, setSortBy] = useState<SortBy>('cost-asc');
  const [redeemingId, setRedeemingId] = useState<string | null>(null);
  const [confirmReward, setConfirmReward] = useState<LoyaltyReward | null>(null);

  useEffect(() => {
    if (user?.id) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const load = async () => {
    setLoading(true);
    try {
      const data = await loyaltyService.getRewards(user!.id);
      setRewards(data);
    } finally {
      setLoading(false);
    }
  };

  const confirmRedeem = async () => {
    if (!user?.id || !confirmReward) return;
    setRedeemingId(confirmReward.id);
    try {
      await loyaltyService.redeemReward(user.id, confirmReward.id);
      toast.success(`Redeemed: ${confirmReward.name}`);
      setConfirmReward(null);
      load();
    } catch {
      /* handled by apiClient */
    } finally {
      setRedeemingId(null);
    }
  };

  const categories = ['all', ...Array.from(new Set(rewards.map((r) => r.category)))];
  const filtered = rewards
    .filter((r) => category === 'all' || r.category === category)
    .sort((a, b) => (sortBy === 'cost-asc' ? a.pointsCost - b.pointsCost : b.pointsCost - a.pointsCost));

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
      <div className="container-wide rewards-catalog-page">
        <h1 className="section-title-loyalty">Rewards</h1>

        <div className="rewards-filters">
          <div className="rewards-filter-group">
            {categories.map((c) => (
              <button key={c} className={`filter-chip ${category === c ? 'active' : ''}`} onClick={() => setCategory(c)}>
                {c === 'all' ? 'All' : c}
              </button>
            ))}
          </div>
          <select className="sort-select" value={sortBy} onChange={(e) => setSortBy(e.target.value as SortBy)}>
            <option value="cost-asc">Points: low to high</option>
            <option value="cost-desc">Points: high to low</option>
          </select>
        </div>

        <div className="rewards-grid">
          {filtered.map((r) => (
            <div key={r.id} className={`reward-card ${!r.affordable ? 'locked' : ''}`}>
              {r.imageUrl && <img src={r.imageUrl} alt={r.name} className="reward-image" />}
              <div className="reward-content">
                <h3 className="reward-title">{r.name}</h3>
                <p className="reward-description">{r.description}</p>
                {r.termsAndConditions && <p className="reward-terms">{r.termsAndConditions}</p>}
                <div className="reward-points">{r.pointsCost} points</div>
                {!r.affordable && <div className="reward-missing">{r.pointsMissing} points missing</div>}
              </div>
              <button
                className="btn btn-outline btn-sm"
                disabled={!r.affordable || redeemingId === r.id}
                onClick={() => setConfirmReward(r)}
              >
                {r.affordable ? 'Redeem' : 'Not enough points'}
              </button>
            </div>
          ))}
          {filtered.length === 0 && <p>No rewards in this category.</p>}
        </div>
      </div>

      {confirmReward && (
        <div className="modal-overlay" onClick={() => setConfirmReward(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Confirm redemption</h3>
            <p>Redeem <strong>{confirmReward.name}</strong> for <strong>{confirmReward.pointsCost} points</strong>?</p>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setConfirmReward(null)}>Cancel</button>
              <button className="btn btn-primary" disabled={redeemingId === confirmReward.id} onClick={confirmRedeem}>
                {redeemingId === confirmReward.id ? 'Redeeming…' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RewardsPage;
