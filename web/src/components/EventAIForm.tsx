import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { eventService } from '../services/event/eventService';
import type { RestaurantOption } from '../services/event/eventService';
import type { EventCategory, GeneratedDescription } from '../types';
import './EventAIForm.css';

const STYLES = [
  'élégant et chaleureux',
  'festif et dynamique',
  'familial et convivial',
  'romantique',
  'décontracté',
];

const CATEGORIES: { value: EventCategory; label: string }[] = [
  { value: 'WINE_TASTING', label: 'Dégustation de vins' },
  { value: 'COOKING_CLASS', label: 'Cours de cuisine' },
  { value: 'LIVE_MUSIC', label: 'Musique live' },
  { value: 'SPECIAL_DINNER', label: 'Dîner spécial' },
  { value: 'THEMED_NIGHT', label: 'Soirée à thème' },
  { value: 'OTHER', label: 'Autre' },
];

const EMPTY = {
  restaurantId: '',
  theme: '',
  eventDate: '',
  cuisine: '',
  places: '',
  price: '',
  category: 'THEMED_NIGHT' as EventCategory,
  menu: '',
  ambiance: '',
  style: STYLES[0],
};

interface Props {
  onSaved?: () => void;
}

export default function EventAIForm({ onSaved }: Props) {
  const [form, setForm] = useState(EMPTY);
  const [restaurants, setRestaurants] = useState<RestaurantOption[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState<GeneratedDescription | null>(null);
  const [hashtagsText, setHashtagsText] = useState('');

  // Chargement de la liste des restaurants
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const list = await eventService.getRestaurants();
        if (active) setRestaurants(list);
      } catch {
        // erreur déjà notifiée par l'intercepteur d'apiClient
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const validate = () => {
    const err: Record<string, string> = {};
    if (!form.restaurantId) err.restaurantId = 'Choisissez un restaurant';
    if (!form.theme.trim()) err.theme = 'Le thème est obligatoire';
    if (!form.eventDate) err.eventDate = "La date et l'heure sont obligatoires";
    if (!form.cuisine.trim()) err.cuisine = 'Le type de cuisine est obligatoire';
    if (!form.places || Number(form.places) <= 0) err.places = 'Nombre de places invalide';
    if (form.price === '' || Number(form.price) < 0) err.price = 'Prix invalide';
    setErrors(err);
    return Object.keys(err).length === 0;
  };

  // Date lisible envoyée à l'IA (ex. "samedi 15 novembre 2026 à 19:30")
  const readableDate = () =>
    new Date(form.eventDate).toLocaleString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  const handleGenerate = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      const data = await eventService.generateDescription({
        theme: form.theme.trim(),
        date: readableDate(),
        cuisine: form.cuisine.trim(),
        menu: form.menu.trim() || undefined,
        ambiance: form.ambiance.trim() || undefined,
        places: Number(form.places),
        style: form.style,
      });
      setResult(data);
      setHashtagsText((data.hashtags ?? []).join(' '));
    } catch {
      // message d'erreur déjà affiché par l'intercepteur (quota, clé, timeout...)
    } finally {
      setLoading(false);
    }
  };

  const currentHashtags = () => hashtagsText.split(/[\s,]+/).filter(Boolean);

  const handleCopy = async () => {
    if (!result) return;
    const text = `${result.titre}\n\n${result.description}\n\n${result.appel_action}\n\n${currentHashtags().join(' ')}`;
    try {
      await navigator.clipboard.writeText(text);
      toast.success('Texte copié pour les réseaux sociaux');
    } catch {
      toast.error('Copie impossible');
    }
  };

  const handleSave = async () => {
    if (!result || !validate()) return;
    setSaving(true);
    try {
      await eventService.create({
        restaurantId: form.restaurantId,
        title: result.titre,
        description: result.description,
        hashtags: currentHashtags(),
        appelAction: result.appel_action,
        generatedByAI: true,
        // datetime-local donne "2026-11-15T19:30" : on ajoute les secondes pour LocalDateTime
        eventDate: form.eventDate.length === 16 ? `${form.eventDate}:00` : form.eventDate,
        price: Number(form.price),
        availableSpots: Number(form.places),
        category: form.category,
        theme: form.theme.trim(),
        cuisine: form.cuisine.trim(),
        menu: form.menu.trim() || undefined,
        ambiance: form.ambiance.trim() || undefined,
      });
      toast.success('Événement enregistré');
      setForm(EMPTY);
      setResult(null);
      setHashtagsText('');
      onSaved?.();
    } catch {
      // erreur déjà notifiée par l'intercepteur
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="ai-form">
      <h2>✨ Nouvel événement avec l'IA</h2>

      <form onSubmit={handleGenerate} noValidate>
        <div className="ai-form__grid">
          <label className="ai-form__full">
            Restaurant *
            <select name="restaurantId" value={form.restaurantId} onChange={handleChange}>
              <option value="">— Choisir —</option>
              {(restaurants ?? []).map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
            {errors.restaurantId && <span className="ai-form__error">{errors.restaurantId}</span>}
          </label>

          <label>
            Thème *
            <input
              name="theme"
              value={form.theme}
              onChange={handleChange}
              placeholder="Soirée jazz et fruits de mer"
              maxLength={100}
            />
            {errors.theme && <span className="ai-form__error">{errors.theme}</span>}
          </label>

          <label>
            Date et heure *
            <input
              type="datetime-local"
              name="eventDate"
              value={form.eventDate}
              onChange={handleChange}
            />
            {errors.eventDate && <span className="ai-form__error">{errors.eventDate}</span>}
          </label>

          <label>
            Type de cuisine *
            <input
              name="cuisine"
              value={form.cuisine}
              onChange={handleChange}
              placeholder="Méditerranéenne"
              maxLength={100}
            />
            {errors.cuisine && <span className="ai-form__error">{errors.cuisine}</span>}
          </label>

          <label>
            Catégorie
            <select name="category" value={form.category} onChange={handleChange}>
              {CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Nombre de places *
            <input
              type="number"
              name="places"
              min={1}
              value={form.places}
              onChange={handleChange}
              placeholder="40"
            />
            {errors.places && <span className="ai-form__error">{errors.places}</span>}
          </label>

          <label>
            Prix par personne *
            <input
              type="number"
              name="price"
              min={0}
              step="0.5"
              value={form.price}
              onChange={handleChange}
              placeholder="45"
            />
            {errors.price && <span className="ai-form__error">{errors.price}</span>}
          </label>

          <label className="ai-form__full">
            Menu
            <textarea
              name="menu"
              value={form.menu}
              onChange={handleChange}
              rows={2}
              maxLength={200}
              placeholder="Poulpe grillé, loup au four, tiramisu"
            />
          </label>

          <label>
            Ambiance
            <input
              name="ambiance"
              value={form.ambiance}
              onChange={handleChange}
              placeholder="Intime, musique live"
              maxLength={100}
            />
          </label>

          <label>
            Style de rédaction
            <select name="style" value={form.style} onChange={handleChange}>
              {STYLES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
        </div>

        <button
          type="submit"
          className="ai-form__btn ai-form__btn--primary"
          disabled={loading || saving}
        >
          {loading ? 'Génération en cours… (quelques secondes)' : "✨ Générer avec l'IA"}
        </button>
      </form>

      {result && (
        <div className="ai-form__result">
          <h3>Résultat (modifiable avant enregistrement)</h3>

          <label>
            Titre
            <input
              value={result.titre}
              onChange={(e) => setResult({ ...result, titre: e.target.value })}
            />
          </label>

          <label>
            Description
            <textarea
              rows={6}
              value={result.description}
              onChange={(e) => setResult({ ...result, description: e.target.value })}
            />
          </label>

          <label>
            Hashtags (séparés par un espace)
            <input value={hashtagsText} onChange={(e) => setHashtagsText(e.target.value)} />
          </label>

          <label>
            Appel à l'action
            <input
              value={result.appel_action}
              onChange={(e) => setResult({ ...result, appel_action: e.target.value })}
            />
          </label>

          <div className="ai-form__actions">
            <button
              type="button"
              className="ai-form__btn"
              onClick={() => handleGenerate()}
              disabled={loading || saving}
            >
              🔄 Régénérer
            </button>
            <button type="button" className="ai-form__btn" onClick={handleCopy}>
              📋 Copier pour les réseaux
            </button>
            <button
              type="button"
              className="ai-form__btn ai-form__btn--primary"
              onClick={handleSave}
              disabled={saving || loading}
            >
              {saving ? 'Enregistrement…' : "✅ Enregistrer l'événement"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}