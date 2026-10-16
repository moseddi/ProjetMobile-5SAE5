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
  menuImage?: string;
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

export type EventCategory =
  | 'WINE_TASTING' | 'COOKING_CLASS' | 'LIVE_MUSIC'
  | 'SPECIAL_DINNER' | 'THEMED_NIGHT' | 'OTHER';

export interface Event {
  id: string;
  restaurantId: string;
  title: string;
  description?: string;
  image?: string;
  eventDate: string;
  price: number;
  availableSpots: number;
  bookedSpots: number;
  category: EventCategory;
  theme?: string;
  cuisine?: string;
  menu?: string;
  ambiance?: string;
  hashtags?: string[];
  appelAction?: string;
  generatedByAI?: boolean;
}

export interface GenerateDescriptionRequest {
  theme: string;
  date: string;
  cuisine: string;
  menu?: string;
  ambiance?: string;
  places?: number;
  style?: string;
}

export interface GeneratedDescription {
  titre: string;
  description: string;
  hashtags: string[];
  appel_action: string;
}

export interface ReviewSnippet {
  label: string;
  rating: number;
  text: string;
  similarityPercent: number;
}

export interface RagItem {
  event: Event;
  restaurantName: string;
  avgRating: number;
  relevancePercent: number;
  explanation: string;
  sources: ReviewSnippet[];
}

export interface RagResponse {
  queryUsed: string;
  mode: 'QUERY' | 'PROFILE' | 'DEFAULT' | 'NONE';
  generated: boolean;
  items: RagItem[];
  message?: string | null;
}