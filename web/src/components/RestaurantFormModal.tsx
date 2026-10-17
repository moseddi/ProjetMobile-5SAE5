import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { resolveRestaurantMediaUrl, restaurantService } from '../services/restaurant/restaurantService';
import { Restaurant } from '../types';
import './RestaurantFormModal.css';

interface RestaurantFormModalProps {
  restaurant?: Restaurant | null;
  onClose: () => void;
  onSaved: (restaurant: Restaurant) => void;
}

const EMPTY_FORM = {
  name: '',
  cuisine: '',
  description: '',
  image: '',
  address: '',
  phone: '',
  openingHours: '',
  priceRange: 'MODERATE' as Restaurant['priceRange'],
  isOpen: true,
  features: '',
};

const RestaurantFormModal = ({ restaurant, onClose, onSaved }: RestaurantFormModalProps) => {
  const isEdit = Boolean(restaurant);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [menuImage, setMenuImage] = useState('');
  const [menuFile, setMenuFile] = useState<File | null>(null);
  const [menuPreview, setMenuPreview] = useState('');

  const fillForm = (data: Restaurant) => {
    setForm({
      name: data.name || '',
      cuisine: data.cuisine || '',
      description: data.description || '',
      image: data.image || '',
      address: data.address || '',
      phone: data.phone || '',
      openingHours: data.openingHours || '',
      priceRange: data.priceRange || 'MODERATE',
      isOpen: data.isOpen ?? true,
      features: data.features?.join(', ') || '',
    });
    setMenuImage(data.menuImage || '');
    setMenuFile(null);
    setMenuPreview(resolveRestaurantMediaUrl(data.menuImage));
  };

  useEffect(() => {
    if (!restaurant) {
      setForm(EMPTY_FORM);
      setMenuImage('');
      setMenuFile(null);
      setMenuPreview('');
      return;
    }

    fillForm(restaurant);

    let cancelled = false;
    restaurantService.getById(restaurant.id)
      .then((fresh) => {
        if (!cancelled) {
          fillForm(fresh);
        }
      })
      .catch((error) => {
        console.error('Failed to reload restaurant:', error);
      });

    return () => {
      cancelled = true;
    };
  }, [restaurant]);

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = event.target;
    const checked = (event.target as HTMLInputElement).checked;
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleMenuFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      toast.error('Please choose an image file for the menu');
      return;
    }
    if (file.size > 12 * 1024 * 1024) {
      toast.error('Menu image must be smaller than 12 MB');
      return;
    }
    setMenuFile(file);
    setMenuPreview(URL.createObjectURL(file));
  };

  const handleRemoveMenu = () => {
    setMenuFile(null);
    setMenuImage('');
    setMenuPreview('');
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (!form.name.trim() || !form.cuisine.trim()) {
      toast.error('Name and cuisine are required');
      return;
    }

    setSaving(true);
    try {
      let uploadedMenuImage = menuImage;
      if (menuFile) {
        try {
          uploadedMenuImage = await restaurantService.uploadMenuImage(menuFile);
        } catch (uploadError) {
          console.error('Failed to upload menu image:', uploadError);
          toast.error('Restaurant will be saved, but the menu image could not be uploaded');
        }
      }

      const payload = {
        name: form.name.trim(),
        cuisine: form.cuisine.trim(),
        description: form.description.trim(),
        image: form.image.trim(),
        menuImage: uploadedMenuImage,
        address: form.address.trim(),
        phone: form.phone.trim(),
        openingHours: form.openingHours.trim(),
        priceRange: form.priceRange,
        isOpen: form.isOpen,
        features: form.features
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean),
        rating: restaurant?.rating ?? 0,
        reviewCount: restaurant?.reviewCount ?? 0,
      };

      const saved = isEdit && restaurant
        ? await restaurantService.update(restaurant.id, { ...restaurant, ...payload })
        : await restaurantService.create(payload);
      toast.success(isEdit ? 'Restaurant updated' : 'Restaurant added');
      onSaved(saved);
      onClose();
    } catch (error) {
      console.error('Failed to save restaurant:', error);
      toast.error(error instanceof Error ? error.message : 'Failed to save restaurant');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="restaurant-modal-overlay" onClick={onClose}>
      <div className="restaurant-modal" onClick={(event) => event.stopPropagation()}>
        <div className="restaurant-modal-header">
          <h2>{isEdit ? 'Edit restaurant' : 'Add restaurant'}</h2>
          <button type="button" className="restaurant-modal-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <form className="restaurant-form" onSubmit={handleSubmit}>
          <div className="restaurant-form-grid">
            <label>
              Name *
              <input name="name" value={form.name} onChange={handleChange} required />
            </label>
            <label>
              Cuisine *
              <input name="cuisine" value={form.cuisine} onChange={handleChange} required />
            </label>
            <label className="restaurant-form-full">
              Description
              <textarea name="description" value={form.description} onChange={handleChange} rows={3} />
            </label>
            <label className="restaurant-form-full">
              Image URL
              <input name="image" value={form.image} onChange={handleChange} placeholder="https://..." />
            </label>
            <div className="restaurant-form-full restaurant-menu-field">
              <span>Menu image</span>
              <p className="restaurant-menu-hint">
                {menuPreview
                  ? 'A menu is already saved. The file box stays empty: choose a file only if you want to replace it.'
                  : 'Upload a photo of the menu from this computer or phone'}
              </p>
              <input
                type="file"
                accept="image/*"
                onChange={handleMenuFileChange}
              />
              {menuPreview ? (
                <div className="restaurant-menu-preview">
                  <img src={menuPreview} alt="Menu preview" />
                  <button type="button" className="btn btn-outline btn-sm" onClick={handleRemoveMenu}>
                    Remove menu
                  </button>
                </div>
              ) : (
                <p className="restaurant-menu-hint">No menu image saved yet.</p>
              )}
            </div>
            <label className="restaurant-form-full">
              Address
              <input name="address" value={form.address} onChange={handleChange} />
            </label>
            <label>
              Phone
              <input name="phone" value={form.phone} onChange={handleChange} />
            </label>
            <label>
              Opening hours
              <input name="openingHours" value={form.openingHours} onChange={handleChange} placeholder="Mon-Sun 12:00-22:00" />
            </label>
            <label>
              Price range
              <select name="priceRange" value={form.priceRange} onChange={handleChange}>
                <option value="BUDGET">BUDGET</option>
                <option value="MODERATE">MODERATE</option>
                <option value="EXPENSIVE">EXPENSIVE</option>
                <option value="LUXURY">LUXURY</option>
              </select>
            </label>
            <label className="restaurant-form-checkbox">
              <input type="checkbox" name="isOpen" checked={form.isOpen} onChange={handleChange} />
              Open now
            </label>
            <label className="restaurant-form-full">
              Amenities (comma separated)
              <input
                name="features"
                value={form.features}
                onChange={handleChange}
                placeholder="Terrace, Parking, Wi-Fi"
              />
            </label>
          </div>

          <div className="restaurant-form-actions">
            <button type="button" className="btn btn-outline" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving...' : isEdit ? 'Save changes' : 'Add restaurant'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RestaurantFormModal;
