# 📑 Guide de Présentation des Tests - HangOut

Ce document sépare clairement les tests réalisés pour **ton module (Avis & RAG)** et ceux réalisés pour le module de **ta binôme (Événements & Description IA)**.

---

## 👤 1. TON MODULE : AVIS & INTELLIGENCE RAG (Seddik)

### Fichiers de Tests :
1. **Tests Unitaires Backend (Java / Mockito) :**
   - 📂 [`backend/src/test/java/com/hangout/service/ReviewServiceTest.java`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout/service/ReviewServiceTest.java)
   - *Rôle :* Vérifie la création d'avis, le calcul multi-critères, les upvotes utiles, et la gestion des restaurants inexistants.

2. **Tests d'Intégration / Contrôleur (Java / MockMvc) :**
   - 📂 [`backend/src/test/java/com/hangout/controller/ReviewControllerIntegrationTest.java`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout/controller/ReviewControllerIntegrationTest.java)
   - *Rôle :* Valide les routes REST `GET /api/reviews`, `GET /api/reviews/restaurant/{id}`, `POST /api/reviews/restaurant/{id}`.

3. **Tests de Détection d'Anomalies (Python / Pytest) :**
   - 📂 [`ai/tests/test_anomaly_detector.py`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/ai/tests/test_anomaly_detector.py)
   - *Rôle :* Détecte les contradictions sémantiques (ex: avis 5 étoiles avec des termes comme "horrible" ou "terrible").

4. **Tests de Pipeline RAG (Python / Pytest) :**
   - 📂 [`ai/tests/test_review_rag_pipeline.py`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/ai/tests/test_review_rag_pipeline.py)
   - *Rôle :* Vérifie l'intégrité du dataset de 100 restaurants, la normalisation des notes sur 5, et le filtrage contextuel.

### 🚀 Comment lancer tes tests en 1 clic :
- Double-clique sur [`run_my_tests.bat`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/run_my_tests.bat)

---

## 👤 2. LE MODULE DE TA BINÔME : ÉVÉNEMENTS & DESCRIPTION IA

### Fichiers de Tests :
1. **Tests Unitaires Backend (Java / Mockito) :**
   - 📂 [`backend/src/test/java/com/hangout/service/EventServiceTest.java`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout/service/EventServiceTest.java)
   - *Rôle :* Vérifie la création d'événements, la mise à jour, la suppression, le filtrage par restaurant, et les exceptions 404.

2. **Tests d'Intégration / Contrôleur (Java / MockMvc) :**
   - 📂 [`backend/src/test/java/com/hangout/controller/EventControllerIntegrationTest.java`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout/controller/EventControllerIntegrationTest.java)
   - *Rôle :* Valide les routes REST `GET /api/events`, `POST /api/events`, et l'endpoint `POST /generate-description`.

3. **Tests Unitaires du Service IA d'Événements (Java / Mockito) :**
   - 📂 [`backend/src/test/java/com/hangout/ai/EventDescriptionServiceTest.java`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/backend/src/test/java/com/hangout/ai/EventDescriptionServiceTest.java)
   - *Rôle :* Vérifie le formatage du prompt système, le parsing JSON de la réponse LLM, et le mécanisme de retry en cas de réponse invalide.

### 🚀 Comment lancer ses tests en 1 clic :
- Double-clique sur [`run_friend_tests.bat`](file:///C:/Users/seddi/Downloads/Flutter/Flutter/run_friend_tests.bat)
