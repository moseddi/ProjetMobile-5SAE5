"""
================================================================================
HangOut AI - Anomaly Detection & Verification Module
================================================================================
Fulfills Academic Rubric:
"Détection d'anomalies pour la vérification du module."

Types of Anomaly Detection:
1. Review Semantic Contradiction: Detects divergence between text sentiment and numerical stars.
2. Review Spam & Bot Detection: Detects gibberish, extreme repetition, or empty content.
3. Event Business Logic Anomalies: Detects overbooking, negative capacity, past dates, price outliers.
================================================================================
"""

import re
from datetime import datetime, timezone
from typing import Dict, Any, List

class ReviewAnomalyDetector:
    """Detects semantic incoherence and spam anomalies in restaurant reviews."""

    NEGATIVE_SIGNALS = {
        'horrible', 'terrible', 'worst', 'disgusting', 'awful', 'poison',
        'inedible', 'rude', 'arrogant', 'dirty', 'scam', 'rip-off', 'stale',
        'catastrophique', 'immangeable', 'inadmissible', 'fuyez', 'voleurs'
    }

    POSITIVE_SIGNALS = {
        'delicious', 'amazing', 'excellent', 'fantastic', 'perfection',
        'outstanding', 'superb', 'best meal', 'divine', 'friendly',
        'délicieux', 'merveilleux', 'parfait', 'exceptionnel', 'adoré'
    }

    @classmethod
    def verify_review(cls, rating: float, comment: str, title: str = "") -> Dict[str, Any]:
        text = f"{title} {comment}".lower()
        words = set(re.findall(r'\b\w+\b', text))

        neg_matches = words.intersection(cls.NEGATIVE_SIGNALS)
        pos_matches = words.intersection(cls.POSITIVE_SIGNALS)

        anomalies: List[str] = []
        anomaly_score = 0.0

        # Anomaly 1: 5 Stars with Heavy Negative Vocabulary (Sarcasm, bot or misclick)
        if rating >= 4.5 and len(neg_matches) >= 2:
            anomaly_score = 0.95
            anomalies.append(
                f"Contradiction Semantique Majeure : Note tres elevee ({rating}/5) "
                f"mais texte contenant des termes fortement negatifs {list(neg_matches)}."
            )

        # Anomaly 2: 1 Star with Heavy Positive Vocabulary (Misclick or trolling)
        elif rating <= 1.5 and len(pos_matches) >= 2:
            anomaly_score = 0.90
            anomalies.append(
                f"Contradiction Semantique Majeure : Note tres basse ({rating}/5) "
                f"mais texte contenant des eloges explicites {list(pos_matches)}."
            )

        # Anomaly 3: Gibberish / Bot Pattern (e.g. 'asdfghjkl' or single character repetition)
        if re.search(r'(.)\1{4,}', text) or (len(words) <= 3 and len(text) > 40):
            anomaly_score = max(anomaly_score, 0.85)
            anomalies.append("Anomalie de Format : Repetition anormale de caracteres ou texte assimile a du spam.")

        # Anomaly 4: Minimalist review without substance
        if len(comment.strip()) < 5:
            anomaly_score = max(anomaly_score, 0.40)
            anomalies.append("Contenu insuffisant pour verification qualitative.")

        is_valid = (anomaly_score < 0.70)
        action = "APPROVE" if is_valid else "FLAG_FOR_MODERATION"

        return {
            "is_valid": is_valid,
            "anomaly_score": round(anomaly_score, 2),
            "anomalies": anomalies,
            "recommended_action": action,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }


class EventAnomalyDetector:
    """Detects business logic and operational anomalies in Events."""

    @classmethod
    def verify_event(cls, event_data: Dict[str, Any]) -> Dict[str, Any]:
        anomalies: List[str] = []
        anomaly_score = 0.0

        available = event_data.get("availableSpots", 0)
        booked = event_data.get("bookedSpots", 0)
        price = event_data.get("price", 0.0)
        date_str = event_data.get("eventDate")

        # Anomaly 1: Overbooking
        if booked > available:
            anomaly_score = 1.0
            anomalies.append(f"Sur-reservation detectee : {booked} places reservees pour {available} disponibles.")

        # Anomaly 2: Negative Capacity
        if available <= 0:
            anomaly_score = max(anomaly_score, 0.90)
            anomalies.append(f"Capacite invalide : {available} places.")

        # Anomaly 3: Price Outlier / Negative Price
        if price < 0:
            anomaly_score = 1.0
            anomalies.append(f"Prix negatif interdit : {price} euros.")
        elif price > 500:
            anomaly_score = max(anomaly_score, 0.75)
            anomalies.append(f"Prix anormalement eleve pour une activite de restauration : {price} euros.")

        # Anomaly 4: Past Date
        if date_str:
            try:
                event_dt = datetime.fromisoformat(str(date_str).replace('Z', ''))
                if event_dt.tzinfo is None:
                    event_dt = event_dt.replace(tzinfo=timezone.utc)
                if event_dt < datetime.now(timezone.utc):
                    anomaly_score = max(anomaly_score, 0.80)
                    anomalies.append(f"Date de l'evenement depassee : {date_str}.")
            except Exception:
                pass

        is_valid = (anomaly_score < 0.70)
        return {
            "is_valid": is_valid,
            "anomaly_score": round(anomaly_score, 2),
            "anomalies": anomalies,
            "recommended_action": "APPROVE" if is_valid else "REJECT_EVENT"
        }


if __name__ == "__main__":
    print("=" * 70)
    print("  HangOut AI - Anomaly Detector Test Demonstration")
    print("=" * 70)

    # Test Case 1: Incoherent Review
    test_review = {
        "rating": 5.0,
        "title": "Une horreur",
        "comment": "The food was terrible and the waiter was rude and arrogant. Worst dinner of my life!"
    }
    report1 = ReviewAnomalyDetector.verify_review(
        test_review["rating"], test_review["comment"], test_review["title"]
    )
    print("\n[Case 1: Review Verification]")
    print(f"Review: {test_review['rating']}/5 | '{test_review['comment']}'")
    print(f"Valid: {report1['is_valid']} | Anomaly Score: {report1['anomaly_score']}")
    print(f"Anomalies: {report1['anomalies']}")
    print(f"Action: {report1['recommended_action']}")

    # Test Case 2: Overbooked Event
    test_event = {
        "title": "Soiree Gastronomique",
        "availableSpots": 20,
        "bookedSpots": 25,
        "price": -10.0
    }
    report2 = EventAnomalyDetector.verify_event(test_event)
    print("\n[Case 2: Event Anomaly Verification]")
    print(f"Valid: {report2['is_valid']} | Anomaly Score: {report2['anomaly_score']}")
    print(f"Anomalies: {report2['anomalies']}")
    print(f"Action: {report2['recommended_action']}")
    print("=" * 70)
