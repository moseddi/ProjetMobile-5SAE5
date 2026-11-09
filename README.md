# 🍽️ HangOut — Restaurant Discovery & Social Dining App

> A full-stack mobile application built with **Flutter** (frontend) and **Spring Boot** (backend), featuring restaurant discovery, table reservations, loyalty rewards, social hangouts, and an **AI-powered review system**.

---
Mentored By MR Khaled Guedria




---

## 🚀 Features

### 👤 For Clients
- 🔍 **Restaurant Discovery** — Browse and search restaurants with filters
- 📅 **Table Reservations** — Book tables and view reservation history
- ⭐ **AI-Powered Reviews** — Write reviews with per-category star ratings analyzed by AI
- 🎉 **Events** — Browse and join restaurant events
- 👥 **HangOut** — Create and join social dining hangouts with friends
- 🎁 **Loyalty Program** — Earn points, play card games and lucky wheel mini-games
- 👤 **Profile Management** — Update personal info and view activity

### 🏪 For Restaurant Owners
- 📊 **Dashboard** — View stats and performance metrics
- 🍽️ **Menu Management** — Add, edit, and remove menu items
- 📆 **Reservation Management** — Accept or reject incoming reservations
- 🎪 **Event Management** — Create and manage restaurant events

### 🛡️ For Admins
- 👥 **User Management** — View and manage all users
- 🏪 **Restaurant Oversight** — Monitor all restaurants on the platform
- 📋 **Owner Applications** — Review and approve/reject owner requests
- 📈 **Platform Statistics** — Global usage analytics

---

## 🤖 AI Features (Review System)

The review system integrates a **multi-agent AI pipeline** on the backend:

| Feature | Description |
|---------|-------------|
| **ABSA** (Aspect-Based Sentiment Analysis) | Reads the review comment and scores 4 aspects: 🍔 Food, 🤝 Service, 💶 Price/Value, 🏠 Ambiance — each rated 1–10 |
| **Anti-Fraud Detection** | Detects contradictions between star ratings and written text (e.g., 5 stars but negative comment). Blocks submission if anomaly found |
| **RAG Recommendations** | If a review is negative, the AI suggests better-matching restaurants using Retrieval-Augmented Generation |

---

## 🏗️ Project Structure

```
Flutter/
├── backend/                        # Spring Boot REST API
│   ├── src/main/java/com/hangout/
│   │   ├── controllers/            # REST endpoints
│   │   ├── services/               # Business logic + AI pipeline
│   │   ├── models/                 # MongoDB documents
│   │   └── repositories/           # Data access layer
│   ├── src/main/resources/
│   │   └── application.properties  # App config (no secrets)
│   └── .env                        # 🔒 Secret keys (gitignored)
│
└── mobile/                         # Flutter App
    └── lib/
        ├── core/
        │   ├── api/                # ApiClient — central HTTP handler
        │   ├── navigation/         # AppRouter — named routes
        │   ├── theme/              # Colors, fonts, theme
        │   └── config/             # AppConfig (base URL)
        ├── models/                 # Dart data models (fromJson/toJson)
        ├── services/
        │   ├── interfaces/         # Abstract service contracts
        │   └── api/                # HTTP implementations
        ├── features/
        │   ├── auth/               # Login, Register, Splash
        │   ├── home/               # Main screen, home feed
        │   ├── restaurants/        # Restaurant list & details
        │   ├── reservations/       # Booking flow
        │   ├── reviews/            # Review list & add review (AI)
        │   ├── events/             # Event browsing
        │   ├── hangout/            # Social hangouts
        │   ├── loyalty/            # Points, card game, lucky wheel
        │   ├── profile/            # User profile
        │   ├── owner/              # Restaurant owner panel
        │   └── admin/              # Admin dashboard
        └── widgets/                # Shared reusable widgets
```

---

## 🛠️ Tech Stack

### Frontend (Flutter)
| Package | Purpose |
|---------|---------|
| `flutter` | UI framework |
| `provider ^6.0.0` | State management |
| `http ^1.1.0` | HTTP/REST API communication |
| `shared_preferences ^2.2.2` | Local JWT token storage |
| `intl ^0.18.0` | Date formatting |

### Backend (Spring Boot)
| Technology | Purpose |
|-----------|---------|
| Java / Spring Boot | REST API framework |
| MongoDB | NoSQL database |
| JWT | Authentication tokens |
| Gemini AI (Google) | Multi-agent AI review pipeline |
| Spring Security | Auth & authorization |

---

## ⚙️ Architecture

### Flutter Architecture — Feature-based with Provider

```
Screen (UI)
  └── context.watch<Provider>()     → reacts to state changes
        └── Provider (ChangeNotifier)
              └── Service Interface (abstract)
                    └── ApiService (HTTP implementation)
                          └── ApiClient (central HTTP handler)
                                └── REST API (Spring Boot)
```

**Key pattern:** Screens never call the API directly. They talk to Providers. Providers talk to service interfaces. This allows swapping implementations without changing any UI code.

### Navigation — Named Routes
All routes are defined as constants in `AppRouter`. Navigation uses `Navigator.of(context).pushNamed()`. Role-based routing sends clients, owners, and admins to different home screens after login.

### Authentication — JWT
On login, the backend returns a JWT token stored in `SharedPreferences`. `ApiClient` automatically attaches `Authorization: Bearer <token>` to every request.

---

## 🚦 User Roles

| Role | Home Screen | Access |
|------|-------------|--------|
| `client` | `MainScreen` | Restaurants, reservations, reviews, events, hangout, loyalty |
| `restaurantOwner` | `OwnerMainScreen` | Menu, reservations, events management |
| `admin` | `AdminMainScreen` | Users, restaurants, owner applications, stats |

---

## 🔧 Setup & Installation

### Prerequisites
- Flutter SDK `>=3.0.0`
- Java 17+
- Maven
- MongoDB (local or Atlas)
- Google Gemini API key

### Backend Setup

```bash
cd backend

# Copy the example env file and fill in your values
cp .env.example .env
```

Edit `.env`:
```properties
MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net/hangout_db
JWT_SECRET=your-very-long-secret-key-at-least-256-bits
GEMINI_API_KEY=your-gemini-api-key-here
```

```bash
# Run the backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

### Flutter Setup

```bash
cd mobile

# Install dependencies
flutter pub get

# Run the app
flutter run
```

> Make sure `AppConfig.apiBaseUrl` in `lib/core/config/app_config.dart` points to your backend URL.

---

## 🔒 Security Notes

- **Never commit secrets** — all API keys and passwords live in `.env` which is gitignored
- JWT tokens are stored locally in `SharedPreferences` and cleared on logout
- The backend validates the JWT on every protected endpoint
- The AI anti-fraud system prevents fake/contradictory reviews

---

## 📡 API Endpoints (Review Feature)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/reviews` | Get all reviews |
| `GET` | `/reviews/restaurant/{id}` | Get reviews for a restaurant |
| `GET` | `/reviews/user/{id}` | Get reviews by a user |
| `GET` | `/reviews/{id}` | Get a single review |
| `POST` | `/reviews/restaurant/{id}` | Create a review (triggers AI pipeline) |
| `PUT` | `/reviews/{id}` | Update a review |
| `DELETE` | `/reviews/{id}` | Delete a review |
| `PUT` | `/reviews/{id}/helpful` | Mark a review as helpful |

---

## 👨‍💻 Authors

Med Iheb Boualagui
Med Malek Chourabi
Ines Missaoui
Cheima Troudi
Med Seddik Hafidh

---

## 📄 License

This project was developed as part of a mobile development course at **ESPRIT SCHOOL OF ENGINEERING**.
