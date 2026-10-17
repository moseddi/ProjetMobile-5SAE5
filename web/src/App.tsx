import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './layouts/Layout';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import RestaurantsPage from './pages/restaurants/RestaurantsPage';
import RestaurantDetailPage from './pages/restaurants/RestaurantDetailPage';
import ReviewsPage from './pages/reviews/ReviewsPage';
import ReservationsPage from './pages/reservations/ReservationsPage';
import ProfilePage from './pages/profile/ProfilePage';
import EventsPage from './pages/events/EventsPage';
import HangOutPage from './pages/hangout/HangOutPage';
import CreateHangOutPage from './pages/hangout/CreateHangOutPage';
import HangOutDetailPage from './pages/hangout/HangOutDetailPage';
import LoyaltyPage from './pages/loyalty/LoyaltyPage';
import GamesPage from './pages/loyalty/GamesPage';
import QuizGame from './pages/loyalty/games/QuizGame';
import MemoryGame from './pages/loyalty/games/MemoryGame';
import ScratchCardGame from './pages/loyalty/games/ScratchCardGame';
import WheelGame from './pages/loyalty/games/WheelGame';
import RewardsPage from './pages/loyalty/RewardsPage';
import HistoryPage from './pages/loyalty/HistoryPage';
import AssistantPage from './pages/loyalty/AssistantPage';
import RecommendationsPage from './pages/loyalty/RecommendationsPage';

/**
 * Entry gate for "/": signed-in users land on the restaurant list, everyone
 * else is sent to the login screen (which offers a "continue as guest" link
 * back to the same browse-only experience).
 */
const HomeGate = () => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
      </div>
    );
  }
  return isAuthenticated ? <Navigate to="/restaurants" replace /> : <Navigate to="/login" replace />;
};

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Pages */}
          <Route path="/" element={<HomeGate />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route element={<Layout />}>
            <Route path="/restaurants" element={<RestaurantsPage />} />
            <Route path="/restaurants/:id" element={<RestaurantDetailPage />} />
            
            {/* Public Pages */}
            <Route path="/reviews" element={<ReviewsPage />} />
            <Route path="/events" element={<EventsPage />} />
            <Route path="/hangout" element={<HangOutPage />} />
            <Route path="/hangout/:id" element={<HangOutDetailPage />} />

            {/* Protected Routes - Require Authentication */}
            <Route element={<PrivateRoute />}>
              <Route path="/reservations" element={<ReservationsPage />} />
              <Route path="/loyalty" element={<LoyaltyPage />} />
              <Route path="/loyalty/games" element={<GamesPage />} />
              <Route path="/loyalty/games/quiz" element={<QuizGame />} />
              <Route path="/loyalty/games/memory" element={<MemoryGame />} />
              <Route path="/loyalty/games/scratch" element={<ScratchCardGame />} />
              <Route path="/loyalty/games/wheel" element={<WheelGame />} />
              <Route path="/loyalty/rewards" element={<RewardsPage />} />
              <Route path="/loyalty/history" element={<HistoryPage />} />
              <Route path="/loyalty/assistant" element={<AssistantPage />} />
              <Route path="/loyalty/recommendations" element={<RecommendationsPage />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/hangout/create" element={<CreateHangOutPage />} />
            </Route>
          </Route>
          
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <ToastContainer position="top-right" autoClose={3000} />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
