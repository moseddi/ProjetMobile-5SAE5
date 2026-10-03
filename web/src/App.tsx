import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider } from './context/AuthContext';
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
import LoyaltyPage from './pages/loyalty/LoyaltyPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Pages */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          
          <Route element={<Layout />}>
            {/* Restaurant List as First Page - PUBLIC */}
            <Route path="/" element={<RestaurantsPage />} />
            <Route path="/restaurants" element={<RestaurantsPage />} />
            <Route path="/restaurants/:id" element={<RestaurantDetailPage />} />
            
            {/* Public Pages */}
            <Route path="/reviews" element={<ReviewsPage />} />
            <Route path="/events" element={<EventsPage />} />
            <Route path="/hangout" element={<HangOutPage />} />
            
            {/* Protected Routes - Require Authentication */}
            <Route element={<PrivateRoute />}>
              <Route path="/reservations" element={<ReservationsPage />} />
              <Route path="/loyalty" element={<LoyaltyPage />} />
              <Route path="/profile" element={<ProfilePage />} />
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
