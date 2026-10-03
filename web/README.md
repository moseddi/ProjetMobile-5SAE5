# HangOut Web Application

Professional web frontend for the HangOut restaurant platform built with React, TypeScript, and Vite.

## Overview

This is a **desktop-first** web application that connects to the same Spring Boot backend as the Flutter mobile app. Both applications share the same database and API, providing a unified experience across platforms.

## Technology Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **React Toastify** - Notifications
- **CSS3** - Styling (no CSS frameworks for custom design)

## Features

### 🏠 Home Page
- Hero section with call-to-action
- Featured restaurants showcase
- Platform features overview
- Professional, warm design

### 🍽️ Restaurant Discovery
- Browse all restaurants with grid layout
- Advanced filtering:
  - Search by name/cuisine/description
  - Filter by cuisine type
  - Filter by price range
  - Show only open restaurants
- Responsive restaurant cards with ratings

### 📋 Restaurant Details
- Full restaurant information
- Image hero section
- Features and amenities
- Customer reviews display
- Sticky sidebar with contact info
- One-click reservation button

### ⭐ Reviews
- View all customer reviews
- Filter by star rating
- Restaurant attribution with images
- Reviewer information and avatars
- Professional review cards

### 📅 Reservations (Protected)
- Create new reservations
- View all reservations
- Filter: All / Upcoming / Past
- Cancel reservations
- Status tracking (Pending, Confirmed, Completed, Cancelled)

### 👤 Profile (Protected)
- User information display
- Account statistics:
  - Loyalty points
  - Total reservations
  - Total orders
  - Total reviews
- Quick action buttons
- Member since date

### 🔐 Authentication
- Login with email/password
- User registration
- JWT token management
- Protected routes
- Auto token refresh
- Persistent sessions

## Project Structure

```
Web-version/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable components
│   │   ├── Navbar.tsx
│   │   ├── PrivateRoute.tsx
│   │   └── RestaurantCard.tsx
│   ├── context/         # React context
│   │   └── AuthContext.tsx
│   ├── layouts/         # Page layouts
│   │   └── Layout.tsx
│   ├── pages/           # Page components
│   │   ├── auth/        # Login, Register
│   │   ├── home/        # Home page
│   │   ├── restaurants/ # Restaurants, Details
│   │   ├── reviews/     # Reviews listing
│   │   ├── reservations/# Reservations management
│   │   └── profile/     # User profile
│   ├── services/        # API services
│   │   ├── api/         # API client
│   │   ├── auth/        # Auth service
│   │   ├── restaurant/  # Restaurant service
│   │   ├── review/      # Review service
│   │   └── reservation/ # Reservation service
│   ├── types/           # TypeScript types
│   ├── App.tsx          # Main app component
│   ├── main.tsx         # App entry point
│   └── index.css        # Global styles
├── .env                 # Environment variables
├── .env.example         # Environment template
├── package.json         # Dependencies
├── tsconfig.json        # TypeScript config
├── vite.config.ts       # Vite config
└── README.md
```

## Installation

### Prerequisites

- Node.js 18+ and npm
- Spring Boot backend running on port 8080
- MongoDB instance running

### Setup

1. **Install dependencies:**
   ```bash
   cd Web-version
   npm install
   ```

2. **Configure environment:**
   ```bash
   # .env file is already created
   # Default: VITE_API_BASE_URL=http://localhost:8080/api
   # Modify if your backend runs on a different port
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

   The app will run on `http://localhost:5173`

4. **Build for production:**
   ```bash
   npm run build
   ```

5. **Preview production build:**
   ```bash
   npm run preview
   ```

## Backend Configuration

Ensure your Spring Boot backend CORS configuration includes the web app:

```java
// backend/src/main/java/com/hangout/config/WebConfig.java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:*",      // Flutter
                "http://10.0.2.2:*",       // Android emulator
                "http://localhost:5173",   // React dev server
                "http://localhost:4173"    // React preview
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
}
```

## Usage

### Demo Credentials

```
Email: demo@example.com
Password: 123456
```

### Development Workflow

1. Start MongoDB
2. Start Spring Boot backend (port 8080)
3. Start React dev server (port 5173)
4. Open browser to `http://localhost:5173`

### API Integration

The app uses a centralized API client with:
- Automatic JWT token injection
- Request/response interceptors
- Error handling
- Timeout management (10 seconds)
- Base URL configuration via environment variables

All API calls go through:
```
React App → API Client → Spring Boot → MongoDB
```

## Design Philosophy

### Professional Restaurant Platform
- Warm neutral color palette (charcoal, gold accents, off-white)
- Desktop-first responsive design
- Large, readable content areas
- Professional photography emphasis
- Clear information hierarchy

### NOT Generic SaaS
- Custom components (no Bootstrap/Material UI)
- Restaurant-specific UI patterns
- Food and dining aesthetics
- Hospitality industry conventions

### User Experience
- Fast navigation
- Clear CTAs
- Immediate feedback
- Smooth transitions
- Responsive interactions

## Available Scripts

```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run preview  # Preview production build
npm run lint     # Run ESLint
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080/api` |

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Architecture Decisions

### Why Vite?
- Fast HMR (Hot Module Replacement)
- Optimized builds
- Native ESM support
- TypeScript out of the box

### Why No CSS Framework?
- Custom brand identity
- Smaller bundle size
- Full design control
- Learning opportunity

### Why Axios?
- Interceptors for auth
- Better error handling
- Request/response transformation
- Built-in timeout support

### State Management
- React Context for auth
- Component state for UI
- No Redux/MobX (not needed for this scale)

## Common Issues

### API Connection Failed
- Ensure backend is running on port 8080
- Check `.env` file has correct `VITE_API_BASE_URL`
- Verify CORS configuration in backend

### Login Not Working
- Check MongoDB is running
- Verify user exists in database
- Check browser console for errors
- Verify JWT secret in backend

### Reviews Not Loading
- Ensure backend has seed data
- Check browser network tab
- Verify review service endpoints

## Next Steps

### Planned Features
- [ ] Pre-order system
- [ ] Loyalty games (Card Game, Lucky Wheel)
- [ ] Events management
- [ ] HangOut social features
- [ ] Notifications
- [ ] User profile editing
- [ ] Restaurant menu display
- [ ] Advanced search
- [ ] Favorites/bookmarks
- [ ] Rating submission

### Improvements
- [ ] Image optimization
- [ ] Lazy loading
- [ ] SEO optimization
- [ ] PWA support
- [ ] Real-time notifications
- [ ] Social sharing

## Testing

```bash
# Run tests (when implemented)
npm run test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

## Contributing

1. Follow TypeScript strict mode
2. Use functional components with hooks
3. Keep components small and focused
4. Write meaningful commit messages
5. Test before committing

## License

Private project - All rights reserved

## Support

For issues or questions, contact the development team.
