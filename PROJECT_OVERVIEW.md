# HangOut – Vue d'ensemble du projet

## C'est quoi HangOut ?

Application complète de **découverte de restaurants**, réservations, fidélité et sorties sociales.
Architecture full-stack : **Backend Java (Spring Boot)** + **Mobile Flutter (Web/Android/iOS)** + **Base de données MongoDB Atlas**.

---

## Rôles utilisateurs

| Rôle | Description |
|------|-------------|
| **CLIENT** | Parcourt les restaurants, réserve, s'inscrit aux événements, crée/rejoint des sorties (Hang Out), laisse des avis, gagne des points de fidélité |
| **RESTAURANT_OWNER** | Gère son restaurant, son menu, ses événements, accepte/refuse les réservations |
| **ADMIN** | Gère les utilisateurs, les restaurants, approuve les demandes d'inscription des owners, modère les avis |

---

## Stack Technologique

### Backend
| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 17 | Langage principal |
| Spring Boot | 3.2.0 | Framework web |
| Spring Data MongoDB | 3.2.0 | Accès base de données |
| Spring Security | 3.2.0 | Sécurité + encodage BCrypt |
| JWT (jjwt) | 0.12.3 | Authentification stateless |
| Lombok | latest | Réduction du code boilerplate |
| Jakarta Validation | latest | Validation des entrées |
| Maven | 3.6+ | Build tool |

### Mobile
| Technologie | Version | Rôle |
|-------------|---------|------|
| Dart | 3.0+ | Langage principal |
| Flutter | 3.35+ | Framework UI multiplateforme |
| Provider | 6.0.0 | Gestion d'état |
| HTTP | 1.1.0 | Appels API REST |
| Shared Preferences | 2.2.2 | Stockage local (token JWT) |
| Intl | 0.18.0 | Internationalisation / dates |

### Base de données
| Technologie | Détail |
|-------------|--------|
| MongoDB Atlas | Cloud NoSQL – hébergé sur Atlas |
| Base : `hangout_db` | 11 collections |

---

## Architecture du projet

```
hangout/
├── backend/                        # API REST Java
│   ├── pom.xml                     # Dépendances Maven
│   ├── .env                        # Variables d'environnement (non versionné)
│   ├── .env.example                # Modèle du fichier .env
│   └── src/main/java/com/hangout/
│       ├── config/                 # CORS, Sécurité, Seeding DB
│       ├── controller/             # 12 contrôleurs REST
│       ├── service/                # 13 services (logique métier)
│       ├── repository/             # 11 repositories MongoDB
│       ├── model/                  # 11 entités MongoDB
│       ├── dto/                    # 20+ objets de transfert de données
│       ├── exception/              # Gestion globale des erreurs
│       ├── scheduler/              # Tâches automatiques (annulation réservations)
│       └── util/                   # Utilitaires
│
└── mobile/                         # Application Flutter
    ├── pubspec.yaml                # Dépendances Flutter
    └── lib/
        ├── core/                   # API client, config, thème, navigation
        ├── features/               # 11 modules fonctionnels
        ├── models/                 # 14 modèles de données
        ├── services/               # Services API
        └── widgets/                # 11 composants réutilisables
```

---

## Collections MongoDB (11)

| Collection | Contenu |
|------------|---------|
| `users` | Comptes & profils utilisateurs |
| `restaurants` | Données restaurants |
| `menu_items` | Produits du menu |
| `reservations` | Réservations de tables et événements |
| `orders` | Pré-commandes |
| `reviews` | Avis clients |
| `events` | Événements des restaurants |
| `hangouts` | Groupes de sorties sociales |
| `game_results` | Résultats des jeux de fidélité |
| `point_transactions` | Historique des points |
| `notifications` | Notifications utilisateurs |

---

## Endpoints REST (Backend)

| Contrôleur | URL de base | Fonctionnalité |
|------------|-------------|----------------|
| AuthController | `/api/auth` | Inscription, connexion |
| RestaurantController | `/api/restaurants` | CRUD restaurants |
| ReservationController | `/api/reservations` | Gestion des réservations |
| ReviewController | `/api/reviews` | Avis clients |
| UserController | `/api/users` | Profils utilisateurs |
| EventController | `/api/events` | Événements |
| HangOutController | `/api/hangouts` | Sorties sociales |
| GameController | `/api/games` | Jeux de fidélité |
| LoyaltyController | `/api/loyalty` | Points de fidélité |
| MenuItemController | `/api/menuitems` | Gestion du menu |
| NotificationController | `/api/notifications` | Notifications |
| OwnerController | `/api/owners` | Opérations owner |
| AdminController | `/api/admin` | Administration |

---

## Fonctionnalités principales

### Flux de réservation
```
Client → Réserve (nom, téléphone, nb personnes)
       → Statut : Pending
Owner  → Accepte ou Refuse
       → Statut : Accepted / Refused
Owner  → Marque "Arrived"
       → +20 points de fidélité au client
Système → Annulation automatique si retard > 30 min
```

### Sorties sociales (Hang Out)
```
Créateur → Crée un groupe (date, restaurant, places)
Rejoignant → Demande à rejoindre → Pending
Créateur → Accepte / Refuse → Notification envoyée
```

### Inscription Owner (workflow admin)
```
Candidat → Remplit formulaire (infos perso + restaurant) → Submit
Admin → Approuve la demande
Système → Crée compte Owner + Restaurant
Owner → Peut se connecter et gérer son restaurant
```

### Système de fidélité
- +20 points à chaque arrivée confirmée par l'owner
- Jeux disponibles : roue de la fortune, jeux de cartes
- Tableau de bord des points pour le client

---

## Sécurité

- **JWT** : Token d'authentification valide 24h (86400000 ms)
- **BCrypt** : Hachage des mots de passe via Spring Security
- **RBAC** : Accès strictement séparé par rôle (CLIENT / OWNER / ADMIN)
- **Validation** : Jakarta Bean Validation sur toutes les entrées
- **CORS** : Configuré pour `localhost:*` et `10.0.2.2:*` (émulateur Android)

---

## Lancer le projet

### Prérequis
- Java 17, Maven
- Flutter 3.35+, support web activé (`flutter config --enable-web`)
- Google Chrome
- Fichier `backend/.env` avec les accès MongoDB Atlas

### Terminal 1 – Backend
```bash
cd backend
# Charger les variables d'environnement (Git Bash)
set -a; source <(grep -v '^#' .env | sed 's/\r$//'); set +a
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```
Attendre le bloc "Demo accounts" dans la console → backend prêt.

### Terminal 2 – Application web
```bash
cd mobile
flutter pub get
flutter run -d web-server --web-port=5000 --web-hostname=0.0.0.0 --dart-define=API_BASE_URL=http://localhost:8081/api
```
Ouvrir **http://localhost:5000** dans le navigateur.

---

## Comptes de test (créés automatiquement au démarrage)

| Rôle | Email | Mot de passe | Restaurant |
|------|-------|--------------|------------|
| ADMIN | `admin@hangout.com` | `admin123` | – |
| OWNER | `owner.legourmet@hangout.com` | `owner123` | Le Gourmet |
| OWNER | `owner.sushi@hangout.com` | `owner123` | Sushi Paradise |
| CLIENT | `demo@example.com` | `123456` | – |
| CLIENT | `john@example.com` | `password` | – |
| CLIENT | `emily@example.com` | `password` | – |

> Pour tester plusieurs comptes simultanément : onglet normal + fenêtre de navigation privée.

---

## Variables d'environnement (.env)

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | URI de connexion MongoDB Atlas |
| `JWT_SECRET` | Clé secrète JWT (minimum 256 bits) |
| `SERVER_PORT` | Port du backend (défaut : 8080) |
| `SPRING_PROFILES_ACTIVE` | Profil Spring (`dev` / `prod`) |
| `APP_SEED_DATA_ENABLED` | Activer les données de démonstration (`true`/`false`) |

---

---

## Événements – Détail complet

### Modèle de données (collection `events`)

| Champ | Type | Description |
|-------|------|-------------|
| `id` | String | Identifiant MongoDB (auto-généré) |
| `restaurantId` | String | Restaurant organisateur |
| `title` | String | Titre de l'événement (obligatoire) |
| `description` | String | Description libre |
| `image` | String | URL de l'image |
| `eventDate` | LocalDateTime | Date et heure (doit être dans le futur) |
| `price` | Double | Prix par personne (≥ 0) |
| `availableSpots` | Integer | Places totales (≥ 1) |
| `bookedSpots` | Integer | Places réservées (incrémenté dès l'inscription, même si encore Pending) |
| `category` | String | Type d'événement (voir ci-dessous) |

### Catégories d'événements

Catégories intégrées (clés fixes) :

| Clé | Label affiché |
|-----|---------------|
| `WINE_TASTING` | Wine Tasting |
| `COOKING_CLASS` | Cooking Class |
| `LIVE_MUSIC` | Live Music |
| `SPECIAL_DINNER` | Special Dinner |
| `THEMED_NIGHT` | Themed Night |
| `OTHER` | Other |

L'owner peut aussi créer des **types libres** (ex. "Soirée karaoké", "Brunch dominical"). Si un même type libre est saisi à nouveau (même casse), il est normalisé vers l'existant pour éviter les doublons dans les filtres.

### Endpoints API (EventController – `/api/events`)

| Méthode | URL | Accès | Description |
|---------|-----|-------|-------------|
| `GET` | `/api/events` | Public | Tous les événements (triés du plus récent) |
| `GET` | `/api/events/{id}` | Public | Détail d'un événement |
| `GET` | `/api/events/restaurant/{restaurantId}` | Public | Événements d'un restaurant |
| `POST` | `/api/events/{id}/register` | CLIENT | S'inscrire à un événement |

Les opérations de création, modification et suppression sont accessibles via `OwnerController` :

| Méthode | URL | Accès | Description |
|---------|-----|-------|-------------|
| `POST` | `/api/owners/events` | RESTAURANT_OWNER | Créer un événement |
| `PUT` | `/api/owners/events/{id}` | RESTAURANT_OWNER | Modifier un événement |
| `DELETE` | `/api/owners/events/{id}` | RESTAURANT_OWNER | Supprimer un événement |

### Flux d'inscription à un événement

```
Client → Ouvre la page de l'événement
       → Clique "Register for Event"
       → Remplit le formulaire : nom, téléphone, nombre de personnes (1–20)
       → La place est IMMÉDIATEMENT réservée (bookedSpots++)
         (pour éviter le double-booking pendant que l'owner décide)
       → Statut de la réservation : Pending

Owner  → Voit la demande dans l'onglet Reservations (filtre Events)
       → Accepte → Statut : Accepted  (notification envoyée au client)
       → Refuse  → Statut : Refused   (places libérées, notification envoyée)

Owner  → Marque "Arrived" → +20 points de fidélité au client
Système → Si retard > 30 min après l'heure acceptée → annulation automatique + notification
```

### Validations (côté backend)

- Titre obligatoire
- Date obligatoirement dans le futur (`@Future`)
- Prix ≥ 0
- Places disponibles ≥ 1
- Catégorie obligatoire (max 40 caractères)
- Un client ne peut pas s'inscrire deux fois au même événement
- Nombre de personnes : entre 1 et 20, et inférieur ou égal aux places restantes
- L'événement doit ne pas être passé au moment de l'inscription

### Interface client (Flutter)

Écran `EventsScreen` – onglet "Events" de l'app :

- Grille de cartes d'événements (les plus récents en premier)
- Panneau de filtres :
  - Recherche textuelle (titre, restaurant, description, catégorie)
  - Filtre par **catégorie** (toutes les catégories intégrées + types libres existants)
  - Filtre par **date** : Upcoming / Today / Next 7 days / plage personnalisée
- Pull-to-refresh

Écran `EventDetailsScreen` – détail d'un événement :

- Image en bannière expansible
- Badge de catégorie + badge places restantes (rouge si complet)
- Infos : restaurant, date, heure, prix par personne, description
- Bouton d'action en bas :
  - "Register for Event" → ouvre le formulaire d'inscription
  - "Request sent - awaiting the restaurant" (après inscription)
  - "Fully booked" (si plus de places)
  - "Event ended" (si la date est passée)

Écran `EventRegistrationScreen` – formulaire d'inscription :

- Carte récapitulative de l'événement (titre, restaurant, date/heure)
- Champs : nom de contact (pré-rempli avec le profil), téléphone, nombre de personnes (stepper)
- Notice d'information : rappel que la demande doit être acceptée + règle des 30 min de retard
- Bouton "Send request"

### Interface owner (Flutter)

Écran `OwnerEventsScreen` – onglet "Events" du tableau de bord owner :

- Même panneau de filtres que côté client (recherche + catégorie + date)
- Tri : plus récent en premier
- Par défaut : tous les événements visibles (y compris les passés)
- Chaque carte affiche les boutons "Edit" et "Delete"
- Bouton flottant "New event"
- Suppression avec confirmation

Écran `OwnerEventFormScreen` – formulaire création/modification :

- Champs : titre, description, image URL, type, date (picker), heure (picker), prix, places disponibles
- Sélecteur de catégorie en chips visuels (icône + label)
- Bouton "New type" pour créer un type libre (via dialog, max 40 caractères)
- Si un type identique existe déjà (même casse), il est réutilisé plutôt que dupliqué
- Validation côté client : tous les champs requis, date dans le futur

### Lien avec les réservations

L'inscription à un événement crée une **réservation de type Event** (même collection `reservations` que les réservations de table). L'owner gère les deux depuis l'onglet "Reservations" avec le filtre "Events / Restaurant". Le client voit ses inscriptions dans "Profil → My Reservations" avec le même filtre.

---

## Notes importantes

- Les données de démo sont créées **automatiquement** au premier démarrage si `APP_SEED_DATA_ENABLED=true`
- Le fichier `.env` ne doit **jamais** être commité (il est dans `.gitignore`)
- Le backend expose ses logs MongoDB en mode `DEBUG` en développement
- Pour les tests, voir le fichier `senario.md` à la racine du projet
