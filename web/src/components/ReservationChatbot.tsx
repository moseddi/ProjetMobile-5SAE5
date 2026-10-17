import { FormEvent, useEffect, useRef, useState } from 'react';
import { chatbotService } from '../services/chatbot/chatbotService';
import {
  ChatResponse,
  ChatTurn,
  EventBookingProposal,
  EventBookingSummary,
  ReservationProposal,
  ReservationSummary,
} from '../types';
import './ReservationChatbot.css';

interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  text: string;
  response?: ChatResponse;
  error?: boolean;
  /** User message refused as out of scope: not sent back as context */
  refused?: boolean;
}

export type ChatbotMode = 'restaurant' | 'event';

interface Props {
  /** Which flow the user opened the assistant for. Only changes the welcome text and quick actions. */
  mode: ChatbotMode;
  /** Restaurant the user came from (Reserve a Table), used as default context */
  restaurantId?: string;
  /** Event the user came from (Reserve an Event), used as default context */
  eventId?: string;
  /** Called after a reservation or event booking was really created, modified or cancelled */
  onReservationChanged?: () => void;
  /** The parent controls visibility: this component is only mounted while open */
  onClose: () => void;
}

const MAX_LENGTH = 500;
const HISTORY_SIZE = 10;

const WELCOME: Record<ChatbotMode, string> = {
  restaurant:
    "Bonjour ! Je suis l'assistant intelligent de réservation. Je peux réserver une table, vérifier les " +
    'disponibilités, modifier ou annuler une réservation. Comment puis-je vous aider ?',
  event:
    "Bonjour ! Je peux vous aider à réserver des places pour un événement, vérifier les disponibilités, " +
    'ou annuler une réservation existante. Comment puis-je vous aider ?',
};

const QUICK_ACTIONS: Record<ChatbotMode, { label: string; message: string }[]> = {
  restaurant: [
    { label: 'Réserver une table', message: 'Je veux réserver une table' },
    { label: 'Vérifier les disponibilités', message: 'Quels sont les créneaux disponibles ce soir ?' },
    { label: 'Modifier une réservation', message: 'Je souhaite modifier ma réservation' },
    { label: 'Mes réservations', message: 'Quelles sont mes réservations ?' },
  ],
  event: [
    { label: 'Quels événements à venir ?', message: 'Quels événements sont prévus ?' },
    { label: 'Réserver un événement', message: 'Je veux réserver une place pour un événement' },
    { label: 'Mes réservations d\'événements', message: 'Quelles sont mes réservations d\'événements ?' },
    { label: 'Annuler une réservation', message: 'Je souhaite annuler ma réservation d\'événement' },
  ],
};

const RESERVATION_STATUS_LABELS: Record<ReservationSummary['status'], string> = {
  PENDING: 'En attente',
  CONFIRMED: 'Confirmée',
  CANCELLED: 'Annulée',
  COMPLETED: 'Terminée',
};

const EVENT_BOOKING_STATUS_LABELS: Record<EventBookingSummary['status'], string> = {
  PENDING: 'En attente',
  CONFIRMED: 'Confirmée',
  CANCELLED: 'Annulée',
};

const formatDateTime = (value?: string | null) =>
  value
    ? new Date(value).toLocaleString('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '';

const formatSlot = (value: string) =>
  new Date(value).toLocaleString('fr-FR', { weekday: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

const ACTION_LABELS: Record<ReservationProposal['action'], string> = {
  CREATE: 'Nouvelle réservation',
  MODIFY: 'Modification',
  CANCEL: 'Annulation',
};

const EVENT_ACTION_LABELS: Record<EventBookingProposal['action'], string> = {
  BOOK: 'Réservation événement',
  CANCEL: 'Annulation',
};

const ReservationChatbot = ({ mode, restaurantId, eventId, onReservationChanged, onClose }: Props) => {
  const [messages, setMessages] = useState<ChatMessage[]>([{ id: 0, role: 'assistant', text: WELCOME[mode] }]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [contextRestaurantId, setContextRestaurantId] = useState<string | undefined>(restaurantId);
  const [contextEventId, setContextEventId] = useState<string | undefined>(eventId);
  // Proposals already confirmed or dismissed, so their buttons cannot be clicked twice
  const [closedProposals, setClosedProposals] = useState<Set<number>>(new Set());
  const nextId = useRef(1);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const push = (message: Omit<ChatMessage, 'id'>) =>
    setMessages((prev) => [...prev, { ...message, id: nextId.current++ }]);

  // History stays in the browser only and is sent back (last messages) so the model keeps the context
  const buildHistory = (): ChatTurn[] =>
    messages
      .filter((m) => m.id !== 0 && !m.error && !m.refused && m.response?.type !== 'OUT_OF_SCOPE')
      .slice(-HISTORY_SIZE)
      .map((m) => ({ role: m.role, content: m.text }));

  const send = async (text: string) => {
    const message = text.trim();
    if (!message || loading) return;
    if (message.length > MAX_LENGTH) return;

    const history = buildHistory();
    const userMessageId = nextId.current;
    push({ role: 'user', text: message });
    setInput('');
    setLoading(true);
    try {
      const response = await chatbotService.sendMessage(message, history, contextRestaurantId, contextEventId);
      if (response.restaurantId) setContextRestaurantId(response.restaurantId);
      if (response.eventId) setContextEventId(response.eventId);
      if (response.type === 'OUT_OF_SCOPE') {
        setMessages((prev) => prev.map((m) => (m.id === userMessageId ? { ...m, refused: true } : m)));
      }
      push({ role: 'assistant', text: response.reply, response });
    } catch (error) {
      push({ role: 'assistant', text: (error as Error).message, error: true });
    } finally {
      setLoading(false);
    }
  };

  const confirm = async (messageId: number, proposal: ReservationProposal) => {
    setClosedProposals((prev) => new Set(prev).add(messageId));
    setLoading(true);
    try {
      const response = await chatbotService.confirm(proposal);
      push({ role: 'assistant', text: response.reply, response });
      if (response.type === 'CONFIRMED') onReservationChanged?.();
    } catch (error) {
      push({ role: 'assistant', text: (error as Error).message, error: true });
    } finally {
      setLoading(false);
    }
  };

  const confirmEvent = async (messageId: number, proposal: EventBookingProposal) => {
    setClosedProposals((prev) => new Set(prev).add(messageId));
    setLoading(true);
    try {
      const response = await chatbotService.confirmEvent(proposal);
      push({ role: 'assistant', text: response.reply, response });
      if (response.type === 'CONFIRMED') onReservationChanged?.();
    } catch (error) {
      push({ role: 'assistant', text: (error as Error).message, error: true });
    } finally {
      setLoading(false);
    }
  };

  const dismiss = (messageId: number) => {
    setClosedProposals((prev) => new Set(prev).add(messageId));
    push({ role: 'assistant', text: "D'accord, rien n'a été modifié. Puis-je vous aider pour autre chose ?" });
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    send(input);
  };

  const renderDetails = (message: ChatMessage) => {
    const response = message.response;
    if (!response) return null;
    const proposal = response.proposal;
    const eventProposal = response.eventProposal;
    const proposalOpen = proposal && !closedProposals.has(message.id) && !loading;
    const eventProposalOpen = eventProposal && !closedProposals.has(message.id) && !loading;

    return (
      <>
        {proposal && (
          <div className="chatbot-proposal">
            <div className="chatbot-proposal-title">{ACTION_LABELS[proposal.action]}</div>
            <dl>
              {proposal.restaurantName && (
                <>
                  <dt>Restaurant</dt>
                  <dd>{proposal.restaurantName}</dd>
                </>
              )}
              {proposal.reservationDateTime && (
                <>
                  <dt>Date</dt>
                  <dd>{formatDateTime(proposal.reservationDateTime)}</dd>
                </>
              )}
              {proposal.partySize && (
                <>
                  <dt>Personnes</dt>
                  <dd>{proposal.partySize}</dd>
                </>
              )}
              {proposal.specialRequests && (
                <>
                  <dt>Demande</dt>
                  <dd>{proposal.specialRequests}</dd>
                </>
              )}
            </dl>
            {proposalOpen && (
              <div className="chatbot-proposal-actions">
                <button type="button" className="btn btn-primary" onClick={() => confirm(message.id, proposal)}>
                  {proposal.action === 'CANCEL' ? "Confirmer l'annulation" : 'Confirmer'}
                </button>
                <button type="button" className="btn btn-outline" onClick={() => dismiss(message.id)}>
                  Non merci
                </button>
              </div>
            )}
          </div>
        )}

        {eventProposal && (
          <div className="chatbot-proposal">
            <div className="chatbot-proposal-title">{EVENT_ACTION_LABELS[eventProposal.action]}</div>
            <dl>
              {eventProposal.eventTitle && (
                <>
                  <dt>Événement</dt>
                  <dd>{eventProposal.eventTitle}</dd>
                </>
              )}
              {eventProposal.eventDate && (
                <>
                  <dt>Date</dt>
                  <dd>{formatDateTime(eventProposal.eventDate)}</dd>
                </>
              )}
              {eventProposal.partySize && (
                <>
                  <dt>Personnes</dt>
                  <dd>{eventProposal.partySize}</dd>
                </>
              )}
            </dl>
            {eventProposalOpen && (
              <div className="chatbot-proposal-actions">
                <button type="button" className="btn btn-primary" onClick={() => confirmEvent(message.id, eventProposal)}>
                  {eventProposal.action === 'CANCEL' ? "Confirmer l'annulation" : 'Confirmer'}
                </button>
                <button type="button" className="btn btn-outline" onClick={() => dismiss(message.id)}>
                  Non merci
                </button>
              </div>
            )}
          </div>
        )}

        {response.suggestedSlots.length > 0 && (
          <div className="chatbot-chips">
            {response.suggestedSlots.map((slot) => (
              <button
                key={slot}
                type="button"
                className="chatbot-chip"
                disabled={loading}
                onClick={() => send(`Je choisis le créneau du ${formatSlot(slot)}`)}
              >
                {formatSlot(slot)}
              </button>
            ))}
          </div>
        )}

        {response.reservations.length > 0 && (
          <ul className="chatbot-reservations">
            {response.reservations.map((r) => (
              <li key={r.id}>
                <span className="chatbot-reservation-number">{r.number}</span>
                <span>
                  {r.restaurantName ?? 'Restaurant'} · {formatDateTime(r.reservationDateTime)} · {r.partySize} pers.
                </span>
                <span className={`chatbot-status chatbot-status-${r.status.toLowerCase()}`}>
                  {RESERVATION_STATUS_LABELS[r.status]}
                </span>
              </li>
            ))}
          </ul>
        )}

        {response.eventBookings.length > 0 && (
          <ul className="chatbot-reservations">
            {response.eventBookings.map((b) => (
              <li key={b.id}>
                <span className="chatbot-reservation-number">{b.number}</span>
                <span>
                  {b.eventTitle ?? 'Événement'} · {formatDateTime(b.eventDate)} · {b.partySize} pers.
                </span>
                <span className={`chatbot-status chatbot-status-${b.status.toLowerCase()}`}>
                  {EVENT_BOOKING_STATUS_LABELS[b.status]}
                </span>
              </li>
            ))}
          </ul>
        )}
      </>
    );
  };

  return (
    <section className="chatbot-panel" aria-label="Assistant intelligent de réservation">
      <header className="chatbot-header">
        <div>
          <h2>Assistant intelligent de réservation</h2>
          <p>
            {mode === 'event'
              ? 'Réservations d\'événements, disponibilités et annulations'
              : 'Réservations, disponibilités, modifications et annulations'}
          </p>
        </div>
        <button type="button" className="chatbot-close" onClick={onClose} aria-label="Fermer">
          ×
        </button>
      </header>

      <div className="chatbot-messages" aria-live="polite">
        {messages.map((m) => (
          <div key={m.id} className={`chatbot-message chatbot-${m.role}${m.error ? ' chatbot-error' : ''}`}>
            <div className="chatbot-bubble">{m.text}</div>
            {m.role === 'assistant' && renderDetails(m)}
          </div>
        ))}

        {messages.length === 1 && (
          <div className="chatbot-chips">
            {QUICK_ACTIONS[mode].map((action) => (
              <button key={action.label} type="button" className="chatbot-chip" onClick={() => send(action.message)}>
                {action.label}
              </button>
            ))}
          </div>
        )}

        {loading && (
          <div className="chatbot-message chatbot-assistant">
            <div className="chatbot-bubble chatbot-typing" aria-label="L'assistant réfléchit">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>

      <form className="chatbot-input" onSubmit={onSubmit}>
        <input
          type="text"
          value={input}
          maxLength={MAX_LENGTH}
          onChange={(e) => setInput(e.target.value)}
          placeholder={mode === 'event' ? 'Ex. : 2 places pour la dégustation de vin' : 'Ex. : une table pour 2 demain à 20h'}
          disabled={loading}
          aria-label="Votre message"
        />
        <button type="submit" className="btn btn-primary" disabled={loading || !input.trim()}>
          Envoyer
        </button>
      </form>
      {input.length > MAX_LENGTH - 50 && (
        <div className="chatbot-counter">
          {input.length}/{MAX_LENGTH}
        </div>
      )}
    </section>
  );
};

export default ReservationChatbot;
