import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import type { HangOut } from '../../types';

const navigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'u1', name: 'Demo User', email: 'demo@example.com' },
    isAuthenticated: true,
    loading: false,
  }),
}));

vi.mock('react-toastify', () => ({
  toast: { info: vi.fn(), success: vi.fn(), error: vi.fn() },
}));

vi.mock('../../services/hangout/hangoutService', () => ({
  hangOutService: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getByCreator: vi.fn(),
    getJoined: vi.fn(),
    create: vi.fn(),
    join: vi.fn(),
    leave: vi.fn(),
    cancel: vi.fn(),
  },
}));

import HangOutPage from './HangOutPage';
import { hangOutService } from '../../services/hangout/hangoutService';

const getAllMock = hangOutService.getAll as unknown as ReturnType<typeof vi.fn>;
const joinMock = hangOutService.join as unknown as ReturnType<typeof vi.fn>;

function makeHangout(overrides: Partial<HangOut> = {}): HangOut {
  return {
    id: 'h1',
    creatorId: 'other-user',
    creatorName: 'Someone Else',
    creatorAvatar: '',
    restaurantId: 'r1',
    restaurantName: 'Bella Italia',
    restaurantImage: '',
    restaurantCuisine: 'Italian',
    restaurantPriceRange: 'MODERATE',
    title: 'Friday Dinner',
    description: 'A nice dinner',
    plannedDateTime: '2999-01-01T19:30:00',
    maxParticipants: 4,
    participants: [],
    joinedCount: 0,
    status: 'OPEN',
    aiGenerated: false,
    createdAt: '2026-01-01T10:00:00',
    ...overrides,
  };
}

describe('HangOutPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the empty state when there are no hangouts', async () => {
    getAllMock.mockResolvedValue([]);

    render(<HangOutPage />);

    expect(await screen.findByText(/No hangouts yet/i)).toBeInTheDocument();
  });

  it('renders a hangout card with its title, restaurant and AI badge', async () => {
    getAllMock.mockResolvedValue([makeHangout({ aiGenerated: true })]);

    render(<HangOutPage />);

    expect(await screen.findByText('Friday Dinner')).toBeInTheDocument();
    expect(screen.getByText('Bella Italia')).toBeInTheDocument();
    expect(screen.getByText('✨ AI')).toBeInTheDocument();
  });

  it('joins a hangout when the Join button is clicked', async () => {
    const hangout = makeHangout();
    getAllMock.mockResolvedValue([hangout]);
    joinMock.mockResolvedValue(hangout);

    render(<HangOutPage />);

    const joinButton = await screen.findByRole('button', { name: /^join$/i });
    fireEvent.click(joinButton);

    await waitFor(() => {
      expect(hangOutService.join).toHaveBeenCalledWith('h1', 'u1');
    });
  });

  it('shows a "Joined" pill when the current user is a participant', async () => {
    getAllMock.mockResolvedValue([
      makeHangout({ participants: [{ userId: 'u1', name: 'Demo User', avatar: '', status: 'ACCEPTED' }], joinedCount: 1 }),
    ]);

    render(<HangOutPage />);

    expect(await screen.findByText('✓ Joined')).toBeInTheDocument();
  });
});
