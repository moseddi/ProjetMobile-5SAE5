# Analyse du projet HangOut

## Objectif

HangOut est une plateforme multi-support de découverte et réservation de restaurants. Elle permet aux utilisateurs de parcourir les restaurants, faire des réservations, laisser des avis, gagner des points de fidélité et organiser des sorties en groupe (HangOuts). Le projet comprend deux clients actifs : une application web React et une application mobile Flutter (non analysée ici), tous deux connectés au même backend Spring Boot et à la même base de données MongoDB.

---

## Fonctionnalités

### Fonctionnalités opérationnelles
- Authentification (inscription, connexion, déconnexion) avec JWT
- Découverte de restaurants avec filtres (cuisine, gamme de prix, ouvert maintenant, recherche textuelle)
- Détail d'un restaurant (infos, horaires, avis, note calculée)
- Système d'avis complet : création, lecture, suppression, marquage "utile"
- Gestion des réservations : création, consultation, annulation, suivi du statut
- Profil utilisateur avec statistiques (points, réservations, commandes, avis)
- Routes protégées nécessitant une authentification

### Fonctionnalités définies mais partiellement implémentées
- Programme de fidélité (points, historique des transactions)
- Jeux de fidélité (Mémoire de cartes, Roue de la fortune)
- Événements organisés par les restaurants
- HangOuts (groupes de sorties sociales)
- Commandes / pré-commandes
- Notifications utilisateur
- Menus des restaurants

---

## Technologies utilisées

### Backend
| Technologie | Rôle |
|---|---|
| Java 17 | Langage |
| Spring Boot 3.2.0 | Framework principal |
| Spring Data MongoDB | Accès base de données |
| Spring Security | Encodage mot de passe (BCrypt) |
| JJWT 0.12.3 | Génération et signature des tokens JWT |
| Jakarta Bean Validation | Validation des entrées |
| Lombok | Réduction du code boilerplate |
| Maven | Gestion des dépendances et build |

### Frontend (web)
| Technologie | Rôle |
|---|---|
| React 18.2.0 | Framework UI |
| TypeScript 5.2.2 | Typage statique |
| Vite 5.0.8 | Outil de build et serveur de dev |
| Axios 1.6.2 | Client HTTP |
| React Router 6.20.0 | Routage côté client |
| React Toastify 9.1.3 | Notifications toast |
| React Icons 4.12.0 | Icônes |
| date-fns 2.30.0 | Manipulation des dates |

---

## Architecture

### Vue d'ensemble

```
Navigateur (React)
       │
       │ HTTP (Axios + Bearer JWT)
       ▼
Spring Boot REST API (port 8081)
       │
       ▼
MongoDB Atlas (base de données hangout_db)
```

### Backend : architecture en couches

```
Controller  →  Service  →  Repository  →  MongoDB
```

- **Controller** : reçoit les requêtes HTTP, délègue au service
- **Service** : logique métier (calcul de note, validation, etc.)
- **Repository** : interface Spring Data MongoDB (requêtes générées automatiquement)
- **DTO** : objets de transfert pour requêtes/réponses (pas d'exposition directe des entités)
- **Exception** : `GlobalExceptionHandler` pour des réponses d'erreur cohérentes (400, 404, 500)

### Frontend : architecture par fonctionnalité

```
App.tsx (routes + AuthProvider)
  └── Layout (Navbar + Outlet)
        └── Pages
              └── Services (apiClient + services métier)
                    └── Types TypeScript partagés
```

- **Context API** : état d'authentification global via `AuthContext`
- **Service layer** : toutes les requêtes API passent par `apiClient.ts`, jamais directement depuis les composants
- **PrivateRoute** : composant wrapper pour les pages nécessitant une authentification

---

## Structure des fichiers importants

### Backend

```
backend/
├── pom.xml                                          ← dépendances Maven
├── src/main/resources/application.properties        ← config (port, MongoDB, JWT, CORS)
└── src/main/java/com/hangout/
    ├── HangOutApplication.java                      ← point d'entrée (@SpringBootApplication)
    ├── config/
    │   ├── SecurityConfig.java                      ← désactive CSRF, sessions stateless
    │   ├── CorsConfig.java                          ← origines autorisées pour le frontend
    │   └── DataSeeder.java                          ← données de test au démarrage
    ├── controller/
    │   ├── AuthController.java                      ← /api/auth/register et /login
    │   ├── RestaurantController.java                ← CRUD restaurants + filtre par cuisine
    │   ├── ReservationController.java               ← créer, voir, annuler
    │   ├── ReviewController.java                    ← CRUD complet + markHelpful
    │   └── UserController.java                      ← profil et réservations par userId
    ├── service/
    │   ├── AuthService.java                         ← register/login, génération JWT
    │   ├── JwtService.java                          ← création du token HS256
    │   ├── RestaurantService.java                   ← logique restaurants
    │   ├── ReservationService.java                  ← logique réservations
    │   ├── ReviewService.java                       ← création + recalcul note restaurant
    │   └── NotificationService.java                 ← notifications
    ├── model/                                       ← 11 entités MongoDB (@Document)
    │   ├── User.java, Restaurant.java, Reservation.java, Review.java
    │   ├── MenuItem.java, Order.java, Event.java, HangOut.java
    │   └── GameResult.java, PointTransaction.java, Notification.java
    ├── repository/                                  ← 11 interfaces Spring Data MongoDB
    ├── dto/
    │   ├── AuthRequest.java, AuthResponse.java, RegisterRequest.java
    │   ├── ReviewRequest.java, ReviewResponse.java, ReservationRequest.java
    │   ├── UserDto.java, GamePlayRequest.java
    └── exception/
        ├── GlobalExceptionHandler.java
        ├── ResourceNotFoundException.java
        └── BadRequestException.java
```

### Frontend (web)

```
web/
├── package.json                                     ← dépendances npm
├── vite.config.ts                                   ← config Vite (port 5173)
├── tsconfig.json                                    ← TypeScript strict mode
└── src/
    ├── App.tsx                                      ← routes publiques/protégées + AuthProvider
    ├── main.tsx                                     ← point d'entrée React
    ├── index.css                                    ← styles globaux
    ├── types/index.ts                               ← toutes les interfaces TypeScript
    ├── context/AuthContext.tsx                      ← état global auth (login/logout/user)
    ├── layouts/Layout.tsx                           ← Navbar + contenu principal
    ├── components/
    │   ├── Navbar.tsx                               ← navigation principale
    │   ├── PrivateRoute.tsx                         ← garde d'authentification
    │   └── RestaurantCard.tsx                       ← carte restaurant réutilisable
    ├── pages/
    │   ├── auth/LoginPage.tsx, RegisterPage.tsx
    │   ├── home/HomePage.tsx
    │   ├── restaurants/RestaurantsPage.tsx, RestaurantDetailPage.tsx
    │   ├── reviews/ReviewsPage.tsx
    │   ├── reservations/ReservationsPage.tsx
    │   ├── profile/ProfilePage.tsx
    │   ├── events/EventsPage.tsx                    ← squelette
    │   ├── hangout/HangOutPage.tsx                  ← squelette
    │   └── loyalty/LoyaltyPage.tsx                  ← squelette
    └── services/
        ├── api/apiClient.ts                         ← client Axios centralisé
        ├── auth/authService.ts                      ← login/register/logout/localStorage
        ├── restaurant/restaurantService.ts
        ├── review/reviewService.ts
        └── reservation/reservationService.ts
```

---

## API (endpoints REST)

### Authentification — `/api/auth`
| Méthode | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Inscription (name, email, password, phone?) |
| POST | `/auth/login` | Connexion → retourne token JWT + objet user |

### Restaurants — `/api/restaurants`
| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/restaurants` | Liste tous les restaurants |
| GET | `/restaurants/{id}` | Détail d'un restaurant |
| GET | `/restaurants/cuisine/{cuisine}` | Filtrer par cuisine |
| POST | `/restaurants` | Créer un restaurant |
| PUT | `/restaurants/{id}` | Modifier un restaurant |
| DELETE | `/restaurants/{id}` | Supprimer un restaurant |

### Réservations — `/api/reservations`
| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/reservations/{id}` | Détail d'une réservation |
| POST | `/reservations?userId={id}` | Créer une réservation |
| DELETE | `/reservations/{id}` | Annuler une réservation |

### Avis — `/api/reviews`
| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/reviews` | Tous les avis |
| GET | `/reviews/{id}` | Un avis par ID |
| GET | `/reviews/restaurant/{id}` | Avis d'un restaurant |
| POST | `/reviews/restaurant/{id}?userId={id}` | Créer un avis |
| PUT | `/reviews/{id}/helpful?userId={id}` | Marquer comme utile |
| DELETE | `/reviews/{id}?userId={id}` | Supprimer un avis |

### Utilisateurs — `/api/users`
| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/users/{id}` | Profil utilisateur |
| GET | `/users/{id}/reservations` | Réservations d'un utilisateur |

---

## Base de données

MongoDB Atlas, base de données `hangout_db`.

| Collection | Champs principaux |
|---|---|
| `users` | email (unique), password (BCrypt), name, phone, avatar, loyaltyPoints, totalReservations, totalOrders, totalReviews |
| `restaurants` | name, cuisine, description, image, address, phone, openingHours, rating (calculé), reviewCount, isOpen, features, priceRange (BUDGET/MODERATE/EXPENSIVE/LUXURY) |
| `reservations` | userId, restaurantId, reservationDateTime, partySize, specialRequests, status (PENDING/CONFIRMED/CANCELLED/COMPLETED) |
| `reviews` | userId, restaurantId, rating (1.0–5.0), title, comment, photoUrl, helpfulCount, status (PUBLISHED/PENDING/REJECTED/DELETED) |
| `menu_items` | restaurantId, name, description, price, category, available |
| `orders` | userId, restaurantId, items, total, status |
| `events` | restaurantId, title, eventDate, price, availableSpots, category |
| `hangouts` | creatorId, restaurantId, participants, maxParticipants, status |
| `game_results` | userId, gameType (CARD_MEMORY/LUCKY_WHEEL), pointsEarned, outcome |
| `point_transactions` | userId, points, type (EARN_RESERVATION, EARN_REVIEW, EARN_GAME, REDEEM…) |
| `notifications` | userId, title, message, type, read, relatedId |

La note d'un restaurant est automatiquement recalculée à chaque ajout ou suppression d'avis (moyenne des notes publiées).

---

## Authentification

### Flux complet

```
1. Formulaire login/inscription
2. authService.login() ou register()
3. POST vers /api/auth/login ou /register
4. Backend valide les données (Jakarta Validation)
5. AuthService vérifie l'email et compare le mot de passe (BCrypt)
6. JwtService génère un token HS256 (expiration : 24h)
7. Réponse : { token, user }
8. Token stocké dans localStorage
9. Toutes les requêtes suivantes → header Authorization: Bearer {token}
10. 401 reçu → logout automatique + redirection /login
```

### Détails techniques
- Mots de passe hashés avec BCrypt via Spring Security
- Sessions stateless (pas de session côté serveur)
- Token JWT signé HS256, contient : userId, email, name, expiration
- Clé secrète configurée via variable d'environnement `JWT_SECRET`
- Côté frontend : `PrivateRoute` vérifie `isAuthenticated` avant d'afficher une page protégée
- Note : la validation du token côté backend n'est pas encore implémentée dans les contrôleurs (toutes les routes sont actuellement `permitAll()`)

### Pages protégées (frontend)
- `/reservations`
- `/loyalty`
- `/profile`

---

## Variables d'environnement

### Backend (`backend/.env`)
| Variable | Description | Obligatoire |
|---|---|---|
| `MONGODB_URI` | URI de connexion MongoDB Atlas | Oui |
| `JWT_SECRET` | Clé secrète JWT (256 bits minimum) | Oui (prod) |

### Frontend (`web/.env`)
| Variable | Description | Défaut |
|---|---|---|
| `VITE_API_BASE_URL` | URL de base de l'API | `http://localhost:8080/api` |

---

## Lancer le projet

### Prérequis
- Java 17+
- Maven 3.6+
- Node.js 18+ et npm
- Compte MongoDB Atlas (ou MongoDB local)

### 1. Backend

```bash
cd backend

# Copier et remplir le fichier .env
cp .env.example .env
# Renseigner MONGODB_URI et JWT_SECRET dans .env

# Compiler et lancer
mvn clean install
mvn spring-boot:run
```

Le backend démarre sur **http://localhost:8081**

Au premier démarrage, des données de test sont insérées automatiquement (3 restaurants, utilisateurs, avis, événements).

Compte de démo : `demo@example.com` / `123456`

### 2. Frontend web

```bash
cd web

# Installer les dépendances
npm install

# Vérifier le fichier .env
# VITE_API_BASE_URL=http://localhost:8081/api

# Lancer le serveur de développement
npm run dev
```

L'application est accessible sur **http://localhost:5173**

### 3. Build de production (web)

```bash
npm run build    # génère le dossier dist/
npm run preview  # prévisualisation du build
```

---

## Notes importantes

- Le backend tourne sur le **port 8081** (défini dans `application.properties`), mais le frontend pointe par défaut sur le port **8080** (dans `apiClient.ts`). Il faut aligner les deux en modifiant `VITE_API_BASE_URL=http://localhost:8081/api` dans le `.env` du frontend.
- La sécurisation JWT côté backend est partiellement implémentée : les tokens sont générés correctement mais ne sont pas encore validés sur les endpoints protégés (toutes les routes sont en `permitAll()`). C'est un point à compléter avant une mise en production.
- Le seeding des données peut être désactivé en mettant `app.seed-data.enabled=false` dans `application.properties`.
- Aucun framework CSS externe n'est utilisé côté web : tout le style est en CSS natif.
