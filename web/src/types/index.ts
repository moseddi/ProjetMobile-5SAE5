// User Types
export interface User {
  id: string;
  email: string;
  name: string;
  phone?: string;
  avatar?: string;
  loyaltyPoints: number;
  totalReservations: number;
  totalOrders: number;
  totalReviews: number;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
  avatar?: string;
}

// Restaurant Types
export interface Restaurant {
  id: string;
  name: string;
  cuisine: string;
  description: string;
  image: string;
  address: string;
  phone: string;
  openingHours: string;
  rating: number;
  reviewCount: number;
  isOpen: boolean;
  features: string[];
  priceRange: 'BUDGET' | 'MODERATE' | 'EXPENSIVE' | 'LUXURY';
  createdAt: string;
  updatedAt: string;
}

// Review Types
export interface Review {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  restaurantId: string;
  restaurantName: string;
  restaurantImage: string;
  rating: number;
  title?: string;
  comment: string;
  photoUrl?: string;
  helpfulCount: number;
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ReviewRequest {
  rating: number;
  title?: string;
  comment: string;
  photoUrl?: string;
}

// Reservation Types
export interface Reservation {
  id: string;
  userId: string;
  restaurantId: string;
  reservationDateTime: string;
  partySize: number;
  specialRequests?: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  createdAt: string;
}

export interface ReservationRequest {
  restaurantId: string;
  reservationDateTime: string;
  partySize: number;
  specialRequests?: string;
}

// API Error Type
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
}
