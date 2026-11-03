# HangOut – Guide de test local et scénarios

Ce document s'adresse à **toute personne qui n'a jamais utilisé l'application**. Il explique comment lancer le projet sur sa machine, avec quels comptes se connecter, et quels scénarios exécuter pour tout tester.

---

## 1. C'est quoi HangOut ?

Une application de restaurants avec **3 rôles** :

| Rôle | Qui est-ce ? | Ce qu'il fait |
|---|---|---|
| **CLIENT** | Un utilisateur normal | Parcourt les restaurants, réserve une table, s'inscrit à des événements, publie / rejoint des sorties (Hang Out), laisse des avis, joue pour gagner des points de fidélité |
| **RESTAURANT_OWNER** | Le propriétaire d'un restaurant | Gère son restaurant, son menu, ses événements, accepte / refuse les réservations, voit les avis |
| **ADMIN** (platform owner) | Le propriétaire de la plateforme | Gère les utilisateurs, les restaurants, les demandes d'inscription des propriétaires, modère les avis, supervise les réservations |

Architecture : **Backend** Spring Boot (Java 21) + base **MongoDB Atlas** ; **Mobile** Flutter, testé dans le **navigateur** (version web).

---

## 2. Prérequis

- **Java 21** et **Maven**
- **Flutter** (3.35 ou plus récent), avec le support web activé (`flutter config --enable-web`)
- **Google Chrome** (ou tout navigateur moderne)
- Le fichier **`backend/.env`** (non versionné, contient les accès MongoDB Atlas). Demandez-le à la personne qui administre le projet et placez-le dans le dossier `backend/`. Le modèle est dans `backend/.env.example`. **Ne le publiez jamais et ne le commitez jamais.**

---

## 3. Lancer le projet

Ouvrez **deux terminaux**.

### Terminal 1 – Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

> On utilise le port **8081** car le 8080 est parfois déjà occupé sur certaines machines. Attendez de voir dans la console le bloc « Demo accounts » (le backend est prêt). Au premier démarrage, des données de démonstration sont créées automatiquement.

Les variables du fichier `.env` doivent être chargées dans l'environnement (par exemple dans Git Bash : `set -a; source <(grep -v '^#' .env | sed 's/\r$//'); set +a` avant la commande Maven).

### Terminal 2 – Application (web)

```bash
cd mobile
flutter pub get
flutter run -d web-server --web-port=5000 --web-hostname=0.0.0.0 --dart-define=API_BASE_URL=http://localhost:8081/api
```

Puis ouvrez **http://localhost:5000** dans le navigateur. Le premier chargement prend un moment.

> Astuce : pour tester **plusieurs comptes en même temps**, utilisez un onglet normal + une **fenêtre de navigation privée** (ou deux navigateurs différents). Chaque fenêtre garde sa propre session.

---

## 4. Comptes de test

| Rôle | Nom | Email | Mot de passe | Remarque |
|---|---|---|---|---|
| ADMIN | Platform Admin | `admin@hangout.com` | `admin123` | Accès à toute la plateforme |
| RESTAURANT_OWNER | Marie Dubois | `owner.legourmet@hangout.com` | `owner123` | Restaurant **Le Gourmet** |
| RESTAURANT_OWNER | Kenji Tanaka | `owner.sushi@hangout.com` | `owner123` | Restaurant **Sushi Paradise** |
| CLIENT | Demo User | `demo@example.com` | `123456` | Compte pré-rempli sur l'écran de login |
| CLIENT | John Smith | `john@example.com` | `password` | |
| CLIENT | Emily Chen | `emily@example.com` | `password` | |

Vous pouvez aussi **créer vos propres comptes** (voir scénarios A1 et B1).

---

## 5. Règles à connaître avant de tester

- **Réservation (table ou événement)** : le client remplit nom, téléphone et nombre de personnes (obligatoires). La demande arrive en **Pending** chez l'owner.
- **Statuts** : Pending → **Accepted** (acceptée) → **Arrived** (le client est arrivé). L'owner peut aussi **Refuse**.
- **Retard** : une réservation acceptée est **annulée automatiquement après 30 minutes de retard** (une notification est envoyée). Passé l'heure, le statut s'affiche **Late**.
- **Points de fidélité** : +20 points quand l'owner marque le client **Arrived**.
- **Hang Out** : le créateur doit **accepter** chaque demande de participation. Il peut aussi la **supprimer** (refuser). Le demandeur est toujours notifié.
- **Ordre d'affichage** : sur tout le site, l'élément **le plus récent est en premier**. Les pages admin ont un tri Newest / Oldest.
- **Rôles étanches** : un client n'accède pas aux écrans owner/admin, un owner ne voit que son restaurant. Une tentative non autorisée renvoie une erreur.

---

## 6. Scénarios de test

Cochez chaque case au fur et à mesure. Si un comportement diffère, notez le scénario, les étapes et le message d'erreur.

### A. Compte client

**A1. Inscription et connexion**
- [ ] Sur la page de login, cliquer sur « Don't have an account? Register », créer un compte (nom, email, mot de passe de 6 caractères minimum).
- [ ] Vous arrivez sur l'accueil client. Se déconnecter (Profil → Logout), puis se reconnecter avec ce compte.
- [ ] Essayer un mauvais mot de passe : un message d'erreur clair doit s'afficher.

**A2. Parcourir les restaurants et les avis**
- [ ] Depuis l'accueil, ouvrir un restaurant : photo, cuisine, horaires, menu.
- [ ] Dans le menu : catégories modernes, produits avec prix.
- [ ] La page du restaurant ne montre pas les avis (ils sont dans l'onglet **Reviews**).
- [ ] Onglet Reviews : lire les avis, trier (Newest / Rating), ajouter un avis avec une note.

**A3. Événements**
- [ ] Onglet Events : utiliser la recherche, le filtre par **type** et par **date** (Upcoming, Today, Next 7 days, plage personnalisée…).
- [ ] Ouvrir un événement et **s'inscrire** : remplir le formulaire (nom, téléphone, nombre de personnes).
- [ ] L'inscription apparaît dans **Profil → My Reservations** avec le statut **Pending**.

**A4. Réservation de table**
- [ ] Sur un restaurant : réserver une table (date, heure, personnes, formulaire de contact obligatoire).
- [ ] Laisser un champ vide : le formulaire doit refuser.
- [ ] Vérifier la réservation dans **My Reservations**, utiliser les filtres **Restaurant / Events** et les statuts.

**A5. Hang Out (sorties entre clients)**
- [ ] Onglet Hang Out : créer une sortie (restaurant, date, nombre de places, message).
- [ ] Utiliser la recherche et les filtres de date (Upcoming, Today, Next 7 days, This month, Past, Custom) et les filtres Toutes / Places libres / Mes publications / Rejoints.
- [ ] Voir **section 7** pour le scénario complet à deux comptes.

**A6. Notifications et fidélité**
- [ ] Profil → **Notifications** : filtrer par type (réservation, événement, hangout, fidélité…).
- [ ] Profil → **My Loyalty** : voir les points. Jouer aux jeux (roue de la chance, jeu de cartes) selon ce qui est disponible.

### B. Compte propriétaire de restaurant

**B1. Demande d'inscription (nouveau propriétaire)**
- [ ] Sur la page de login, cliquer sur **« Restaurant owner? Register your restaurant »**.
- [ ] Remplir « About you » (nom, email, téléphone, mot de passe + confirmation) et « Your restaurant » (nom, cuisine, description, adresse, etc.).
- [ ] Après l'envoi, un écran **« Request sent! »** indique d'attendre l'acceptation de l'admin (pas d'e-mail pour le moment).
- [ ] Essayer de se connecter avec cet email : message « still waiting for admin approval ».
- [ ] Voir le scénario C3 pour la partie admin, puis se reconnecter : l'owner arrive sur son tableau de bord.

**B2. Tableau de bord et restaurant**
- [ ] Se connecter avec `owner.legourmet@hangout.com` / `owner123`.
- [ ] Voir les statistiques, modifier le profil du restaurant (description, horaires, ouvert / fermé).

**B3. Menu**
- [ ] Ajouter un produit avec une **catégorie libre** (par exemple « Desserts maison », pas seulement les catégories prédéfinies).
- [ ] Modifier et supprimer un produit. Côté client, le menu affiche la nouvelle catégorie.

**B4. Événements**
- [ ] Créer un événement avec un **type libre** (par exemple « Soirée karaoké »).
- [ ] Utiliser la recherche et les filtres type / date. Modifier, supprimer.
- [ ] Côté client, l'événement apparaît (le plus récent en premier).

**B5. Réservations (le plus important)**
- [ ] Onglet Reservations : filtrer **Restaurant / Events** et par statut.
- [ ] Sur une demande Pending : **Accept** ou **Refuse**. Le client reçoit une notification.
- [ ] Sur une réservation acceptée : **Arrived** (le client gagne +20 points).
- [ ] Réservation acceptée dont l'heure est dépassée depuis plus de 30 minutes : elle devient **Late** puis est annulée automatiquement, avec notification.

**B6. Avis**
- [ ] Onglet Reviews : consulter les avis reçus par son restaurant.

**B7. Isolation entre owners**
- [ ] Se connecter avec `owner.sushi@hangout.com` : on ne voit que les données de Sushi Paradise, jamais celles de Le Gourmet.

### C. Compte administrateur (platform owner)

Se connecter avec `admin@hangout.com` / `admin123`. Barre du bas : Dashboard, Requests, Users, Restaurants, Reviews, Profile.

**C1. Dashboard**
- [ ] Voir les compteurs : utilisateurs, restaurants, restaurants sans owner, réservations, avis, hangouts, événements.
- [ ] Si des demandes d'owners sont en attente, une bannière orange apparaît et ouvre la liste.

**C2. Créer un restaurant et lui affecter un owner (choix 1)**
- [ ] Restaurants → bouton **+**. Remplir les champs du restaurant.
- [ ] Section **Restaurant owner** :
  - **Later** : restaurant sans owner (on pourra l'assigner plus tard).
  - **New account** : saisir nom, email, mot de passe de l'owner. Le compte est créé avec le restaurant.
  - **Existing** : choisir un compte client existant, qui devient owner.
- [ ] Se déconnecter et se connecter avec le nouveau compte owner : il gère son restaurant.

**C3. Accepter ou refuser une demande d'owner (choix 2)**
- [ ] Faire d'abord le scénario B1 pour créer une demande.
- [ ] Onglet **Requests** (un badge indique le nombre en attente) : voir la carte du restaurant, de l'owner, les contacts.
- [ ] **Accept** : le restaurant et le compte owner sont créés. L'owner peut se connecter immédiatement.
- [ ] **Reject** : saisir un motif facultatif. L'owner verra ce motif en essayant de se connecter.
- [ ] Filtres Pending / Accepted / Rejected / All, recherche, tri Newest / Oldest.

**C4. Gérer les utilisateurs**
- [ ] Onglet Users : rechercher, filtrer par rôle (Clients / Owners / Admins) et par statut (Active / Deactivated), trier.
- [ ] Ouvrir un utilisateur : désactiver / réactiver son compte (un compte désactivé ne peut plus se connecter), assigner un restaurant, changer le rôle, supprimer.
- [ ] Vérifier qu'on ne peut pas désactiver ni supprimer son propre compte.

**C5. Gérer les restaurants**
- [ ] Onglet Restaurants : recherche, filtres All / Needs owner / Has owner / Open / Closed.
- [ ] Modifier un restaurant, supprimer un restaurant (confirmation demandée).

**C6. Modérer les avis et superviser les réservations**
- [ ] Onglet Reviews : filtrer par note, rechercher, supprimer un avis inapproprié.
- [ ] Depuis le dashboard : voir **toutes les réservations** (filtres Restaurant / Events / statuts, recherche, tri).

---

## 7. Scénarios multi-comptes (à faire à plusieurs)

Ces scénarios nécessitent **deux fenêtres** (normale + privée) ou **deux personnes**.

### Scénario 1 – Hang Out : rejoindre une sortie
1. **Fenêtre 1 – John** (`john@example.com`) : Hang Out → **+** → publier une sortie (restaurant, date future, 3 places).
2. **Fenêtre 2 – Emily** (`emily@example.com`) : Hang Out → trouver la sortie de John → **Join**.
   - Emily reçoit un message « Wait for John to accept » et, dans ses notifications, **« Request sent – waiting for John »**.
   - Le bouton devient « Waiting for John to accept ».
3. **John** : Profil → **My Hang Outs** → onglet **Requests** (badge sur le profil) : il voit la demande d'Emily.
   - Cliquer **Accept** → Emily reçoit **« Request accepted! »** et apparaît parmi les membres.
   - (Ou **Delete** → Emily reçoit **« Request declined »**.)
4. **Demo User** demande aussi à rejoindre, puis John accepte : **Emily** (déjà membre) reçoit **« New member »**.
5. Vérifier la limite de places : quand la sortie est pleine, le bouton Join devient **Full**.
6. Cas d'erreur : John ne peut pas rejoindre sa propre sortie ; on ne peut pas redemander après une demande déjà envoyée.

### Scénario 2 – Réservation de bout en bout
1. **Client (Demo)** réserve une table chez **Le Gourmet** : Pending.
2. **Owner Le Gourmet** (fenêtre privée) : Reservations → **Accept**.
3. **Client** : notification d'acceptation, statut **Accepted** dans My Reservations.
4. **Owner** : **Arrived** → le client reçoit **+20 points** (Profil → My Loyalty).
5. Variante : l'owner clique **Refuse** → le client est notifié, le statut devient Refused.

### Scénario 3 – Inscription à un événement
1. **Owner** crée un événement avec un type libre.
2. **Client** s'y inscrit (formulaire obligatoire) : statut **Pending** dans My Reservations.
3. **Owner** accepte, puis marque **Arrived** ; les places disponibles de l'événement se mettent à jour.

### Scénario 4 – Nouveau propriétaire, de la demande à la connexion
1. **Visiteur** : login → « Restaurant owner? Register your restaurant » → envoyer la demande.
2. Le visiteur essaie de se connecter : **refusé avec message d'attente**.
3. **Admin** : onglet Requests (badge) → **Accept**.
4. **Le nouvel owner** se connecte : il gère son nouveau restaurant, qui apparaît dans la liste côté client.

### Scénario 5 – Compte désactivé
1. **Admin** désactive le compte d'Emily (Users).
2. **Emily** essaie de se connecter : message « account has been deactivated ».
3. **Admin** le réactive : la connexion fonctionne à nouveau.

### Scénario 6 – Droits et limites (sécurité)
- [ ] Un client connecté ne voit aucun écran owner ou admin.
- [ ] L'owner de Sushi Paradise ne peut pas modifier le restaurant ou les réservations de Le Gourmet.
- [ ] Un hangout ne peut être géré (accepter / supprimer des demandes) que par son créateur.

---

## 8. Problèmes fréquents

| Symptôme | Solution |
|---|---|
| La page reste blanche ou « connection refused » | Vérifier que le backend est lancé (terminal 1) sur le port **8081**, et que `--dart-define=API_BASE_URL=http://localhost:8081/api` est bien passé à Flutter |
| `Port 8081 was already in use` | Un ancien backend tourne encore : arrêtez-le, ou changez de port (et mettez le même dans `API_BASE_URL`) |
| Erreur de connexion à MongoDB au démarrage | Le fichier `backend/.env` est absent ou incorrect, ou votre IP n'est pas autorisée dans MongoDB Atlas |
| Le login affiche « Invalid email or password » | Vérifiez l'email et le mot de passe dans la section 4 ; les comptes de démonstration sont créés au premier démarrage du backend |
| Les modifications de code mobile n'apparaissent pas | Recharger complètement la page (Ctrl + Maj + R) ou relancer `flutter run` |
| Les comptes ou données de test sont mélangés | Les données sont partagées par tous les testeurs (même base). Préfixez vos données de test (par exemple « TEST-prenom ») et supprimez-les ensuite |

> La base MongoDB est **partagée** : ce que fait un membre du groupe est visible par les autres. Évitez de supprimer les comptes de démonstration de la section 4.
