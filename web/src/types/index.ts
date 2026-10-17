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
  seatingCapacity?: number;
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

export interface EventBooking {
  id: string;
  userId: string;
  eventId: string;
  partySize: number;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
}

export interface AvailabilityResult {
  available: boolean;
  restaurantId: string;
  requestedDateTime: string;
  partySize: number;
  remainingSeats: number;
  reason: 'AVAILABLE' | 'NOT_ENOUGH_SEATS' | 'OUTSIDE_OPENING_HOURS' | 'RESTAURANT_CLOSED' | 'PARTY_TOO_LARGE_FOR_RESTAURANT';
  alternatives: string[];
}

// Reservation assistant (chatbot) Types
export type ChatIntent = 'CREATE' | 'CHECK_AVAILABILITY' | 'MODIFY' | 'CANCEL' | 'STATUS' | 'INFO' | 'OUT_OF_SCOPE';
export type ChatResponseType = 'ANSWER' | 'QUESTION' | 'PROPOSAL' | 'CONFIRMED' | 'OUT_OF_SCOPE';
export type ProposalAction = 'CREATE' | 'MODIFY' | 'CANCEL';

export interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
}

export interface ReservationProposal {
  action: ProposalAction;
  reservationId?: string | null;
  restaurantId?: string | null;
  restaurantName?: string | null;
  reservationDateTime?: string | null;
  partySize?: number | null;
  specialRequests?: string | null;
}

export interface ReservationSummary {
  number: number;
  id: string;
  restaurantId: string;
  restaurantName?: string | null;
  reservationDateTime: string;
  partySize: number;
  status: Reservation['status'];
}

export type EventProposalAction = 'BOOK' | 'CANCEL';

export interface EventBookingProposal {
  action: EventProposalAction;
  bookingId?: string | null;
  eventId?: string | null;
  eventTitle?: string | null;
  eventDate?: string | null;
  partySize?: number | null;
}

export interface EventBookingSummary {
  number: number;
  id: string;
  eventId: string;
  eventTitle?: string | null;
  eventDate: string;
  partySize: number;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
}

export interface ChatResponse {
  reply: string;
  intent: ChatIntent | null;
  type: ChatResponseType;
  proposal: ReservationProposal | null;
  suggestedSlots: string[];
  reservations: ReservationSummary[];
  restaurantId: string | null;
  eventProposal: EventBookingProposal | null;
  eventBookings: EventBookingSummary[];
  eventId: string | null;
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

// HangOut Types
export type HangOutStatus = 'OPEN' | 'FULL' | 'CANCELLED' | 'COMPLETED';
export type ParticipantStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface HangOutParticipant {
  userId: string;
  name: string;
  avatar: string;
  status: ParticipantStatus;
}

export interface HangOut {
  id: string;
  creatorId: string;
  creatorName: string;
  creatorAvatar: string;
  restaurantId: string;
  restaurantName: string;
  restaurantImage: string;
  restaurantCuisine: string;
  restaurantPriceRange: string;
  title: string;
  description: string;
  plannedDateTime: string;
  maxParticipants: number;
  participants: HangOutParticipant[];
  joinedCount: number;
  status: HangOutStatus;
  aiGenerated: boolean;
  createdAt: string;
}

export interface HangOutRequest {
  restaurantId: string;
  title: string;
  description?: string;
  plannedDateTime: string;
  maxParticipants: number;
  aiGenerated?: boolean;
}

// AI Hangout Assistant Types
export interface AiHangoutRequest {
  prompt: string;
  partySize?: number;
  budget?: string;
}

export interface AiHangoutResponse {
  restaurantId: string;
  restaurantName: string;
  title: string;
  description: string;
  suggestedDateTime: string;
  keywords: string[];
  reason: string;
  suggestedPartySize: number;
  aiGenerated: boolean;
  provider: string;
  notice?: string | null;
}

// Loyalty Types
export interface LoyaltyPointTransaction {
  id: string;
  points: number;
  type: string;
  description: string;
  createdAt: string;
}

export interface LoyaltyChallenge {
  id: string;
  title: string;
  description: string;
  pointsReward: number;
  progress: number;
  target: number;
  completed: boolean;
}

export interface LoyaltyGame {
  id: string;
  name: string;
  description: string;
  category: string;
  difficulty: string;
  estimatedDurationMinutes: number;
  icon: string;
  rules: string;
  pointsReward: string;
  dailyLimit: number;
  attemptsUsedToday: number;
  availableToday: boolean;
}

export interface LoyaltyDashboard {
  points: number;
  tier: string;
  nextTier: string | null;
  pointsToNextTier: number;
  tierProgressPercent: number;
  recentTransactions: LoyaltyPointTransaction[];
  affordableRewardsCount: number;
  todaysChallenges: LoyaltyChallenge[];
  games: LoyaltyGame[];
}

export interface LoyaltyReward {
  id: string;
  name: string;
  description: string;
  category: string;
  pointsCost: number;
  stock: number | null;
  imageUrl: string;
  termsAndConditions: string;
  affordable: boolean;
  pointsMissing: number;
}

export interface LoyaltyRedemption {
  id: string;
  rewardId: string;
  rewardName: string;
  pointsCost: number;
  status: string;
  createdAt: string;
}

export interface RestaurantRecommendationCard {
  restaurantId: string;
  restaurantName: string;
  restaurantImage: string | null;
  cuisine: string | null;
  priceRange: string | null;
  rating: number | null;
  rewardId: string;
  rewardName: string;
  rewardCategory: string | null;
  pointsCost: number;
  affordable: boolean;
  pointsMissing: number;
  pointsRemainingAfterRedemption: number | null;
  rewardExpiryDate: string | null;
  rewardTerms: string | null;
  reason: string | null;
  action: 'VIEW_REWARD' | 'VIEW_RESTAURANT';
}

export interface RestaurantRecommendationResponse {
  points: number;
  goal: string | null;
  message: string | null;
  recommendations: RestaurantRecommendationCard[];
  aiGenerated: boolean;
  notice: string | null;
}

export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  category: string;
  difficulty: string;
}

export interface QuizSubmitRequest {
  answers: { questionId: string; selectedIndex: number }[];
}

export interface GameResultResponse {
  gameType: string;
  score?: number;
  correctAnswers?: number;
  totalQuestions?: number;
  pointsEarned: number;
  newBalance: number;
  outcome: string;
  message: string;
  explanations?: {
    questionId: string;
    correct: boolean;
    correctAnswerIndex: number;
    explanation: string;
  }[];
}

export interface MemoryStartResponse {
  sessionId: string;
  pairCount: number;
  symbols: string[];
}

export interface AssistantRecommendation {
  type: 'GAME' | 'REWARD';
  id: string;
  title: string;
  reason: string;
  action: 'VIEW_GAME' | 'START_GAME' | 'VIEW_REWARD' | 'REDEEM_REWARD';
}

export interface AssistantChatResponse {
  intent: string;
  message: string;
  recommendations: AssistantRecommendation[];
  followUpQuestion: string | null;
  aiGenerated: boolean;
  notice?: string | null;
}