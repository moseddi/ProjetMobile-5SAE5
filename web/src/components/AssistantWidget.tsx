import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { assistantService, DishMatch } from '../services/assistant/assistantService';
import './AssistantWidget.css';

type Tab = 'chat' | 'photo';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  imageUrl?: string;
  matches?: DishMatch[];
}

const SUGGESTIONS = [
  'Où manger un burger ?',
  'Des desserts ?',
  'Du poisson ?',
];

const AssistantWidget = () => {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<Tab>('chat');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      text: 'Bonjour ! Demandez-moi un plat, un restaurant, ou envoyez une photo de menu.',
    },
  ]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState('');
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, open, busy]);

  const addMessage = (message: Omit<ChatMessage, 'id'>) => {
    const next = { ...message, id: `${Date.now()}-${Math.random()}` };
    setMessages((prev) => [...prev, next]);
    return next;
  };

  const handleChat = async (text: string) => {
    const question = text.trim();
    if (!question || busy) {
      return;
    }

    setInput('');
    addMessage({ role: 'user', text: question });
    setBusy(true);
    try {
      const answer = await assistantService.chat(question);
      addMessage({ role: 'assistant', text: answer });
    } catch (error) {
      addMessage({
        role: 'assistant',
        text: error instanceof Error ? error.message : "L'assistant est indisponible pour le moment.",
      });
    } finally {
      setBusy(false);
    }
  };

  const handleChatSubmit = (event: FormEvent) => {
    event.preventDefault();
    handleChat(input);
  };

  const handlePhotoPick = (file?: File) => {
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      addMessage({ role: 'assistant', text: 'Choisissez une image (JPG, PNG, WEBP).' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      addMessage({ role: 'assistant', text: 'Image trop lourde (5 Mo max).' });
      return;
    }
    setPhotoFile(file);
    setPhotoPreview(URL.createObjectURL(file));
  };

  const handlePhotoSearch = async () => {
    if (!photoFile || busy) {
      return;
    }

    addMessage({ role: 'user', text: 'Recherche à partir de cette photo', imageUrl: photoPreview });
    setBusy(true);
    try {
      const result = await assistantService.searchByPhoto(photoFile);
      addMessage({
        role: 'assistant',
        text: result.answer || result.description,
        matches: result.matches,
      });
      setTab('chat');
    } catch (error) {
      addMessage({
        role: 'assistant',
        text: error instanceof Error ? error.message : 'Impossible d’analyser la photo.',
      });
    } finally {
      setBusy(false);
      setPhotoFile(null);
      setPhotoPreview('');
    }
  };

  return (
    <div className="assistant-root">
      {open && (
        <section className="assistant-panel" aria-label="Assistant HangOut">
          <header className="assistant-header">
            <div>
              <h2>Assistant HangOut</h2>
              <p>Chat et recherche par photo</p>
            </div>
            <button type="button" className="assistant-close" onClick={() => setOpen(false)} aria-label="Fermer">
              ×
            </button>
          </header>

          <div className="assistant-tabs">
            <button
              type="button"
              className={tab === 'chat' ? 'active' : ''}
              onClick={() => setTab('chat')}
            >
              Chat
            </button>
            <button
              type="button"
              className={tab === 'photo' ? 'active' : ''}
              onClick={() => setTab('photo')}
            >
              Photo
            </button>
          </div>

          {tab === 'chat' ? (
            <>
              <div className="assistant-messages" ref={listRef}>
                {messages.map((message) => (
                  <div key={message.id} className={`assistant-bubble ${message.role}`}>
                    {message.imageUrl && (
                      <img src={message.imageUrl} alt="Photo envoyée" className="assistant-thumb" />
                    )}
                    <p>{message.text}</p>
                    {message.matches && message.matches.length > 0 && (
                      <ul className="assistant-matches">
                        {message.matches.map((match, index) => (
                          <li key={`${match.restaurantId}-${match.dishName}-${index}`}>
                            {match.restaurantId ? (
                              <Link to={`/restaurants/${match.restaurantId}`} onClick={() => setOpen(false)}>
                                {match.dishName || 'Plat'}
                              </Link>
                            ) : (
                              <strong>{match.dishName || 'Plat'}</strong>
                            )}
                            {match.restaurantName && <span> · {match.restaurantName}</span>}
                            {match.price != null && <span> · {match.price}</span>}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                ))}
                {busy && (
                  <div className="assistant-bubble assistant typing">
                    <span /><span /><span />
                  </div>
                )}
              </div>

              <div className="assistant-suggestions">
                {SUGGESTIONS.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    disabled={busy}
                    onClick={() => handleChat(suggestion)}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>

              <form className="assistant-composer" onSubmit={handleChatSubmit}>
                <input
                  value={input}
                  onChange={(event) => setInput(event.target.value)}
                  placeholder="Ex. Où manger un burger ?"
                  disabled={busy}
                />
                <button type="submit" className="btn btn-primary btn-sm" disabled={busy || !input.trim()}>
                  Envoyer
                </button>
              </form>
            </>
          ) : (
            <div className="assistant-photo">
              <p>Envoyez une photo de plat depuis votre ordinateur ou téléphone. L’assistant cherchera les menus indexés.</p>
              <label className="assistant-file">
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => handlePhotoPick(event.target.files?.[0])}
                />
                Choisir une photo
              </label>
              {photoPreview && (
                <div className="assistant-photo-preview">
                  <img src={photoPreview} alt="Aperçu du plat" />
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={handlePhotoSearch}
                    disabled={busy}
                  >
                    {busy ? 'Analyse...' : 'Chercher ce plat'}
                  </button>
                </div>
              )}
            </div>
          )}
        </section>
      )}

      <button
        type="button"
        className={`assistant-fab ${open ? 'open' : ''}`}
        onClick={() => setOpen((value) => !value)}
        aria-label={open ? 'Fermer l’assistant' : 'Ouvrir l’assistant'}
      >
        {open ? '×' : 'AI'}
      </button>
    </div>
  );
};

export default AssistantWidget;
